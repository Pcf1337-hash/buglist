package com.buglist.presentation.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Result
import com.buglist.domain.model.SettlementResult
import com.buglist.domain.usecase.SettleDebtsUseCase
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents a single row in the live settlement preview list.
 *
 * @param entry            The debt entry.
 * @param amountToPay      The amount that will be applied to this entry. 0 if not touched.
 * @param resultingStatus  The status the entry will have after the settlement.
 * @param isTouched        true if this entry's balance is reduced by the current input amount.
 */
data class SettlementPreviewItem(
    val entry: DebtEntry,
    val currentRemaining: Double,
    val amountToPay: Double,
    val resultingStatus: DebtStatus,
    val isTouched: Boolean
)

/**
 * UI state for the SettlementSheet.
 */
sealed class SettlementUiState {
    /** Initial state — open debts loaded, waiting for user input. */
    data class Idle(
        val totalOpen: Double,
        val openCount: Int
    ) : SettlementUiState()

    /** User is entering an amount and the live preview is updating. */
    data class Preview(
        val inputAmount: Double,
        val totalOpen: Double,
        val openCount: Int,
        val preview: List<SettlementPreviewItem>
    ) : SettlementUiState()

    /** Settlement transaction is running. */
    object Processing : SettlementUiState()

    /** Settlement completed successfully — triggers sheet close in the UI. */
    data class Success(val result: SettlementResult) : SettlementUiState()

    /** An error occurred. */
    data class Error(val message: String) : SettlementUiState()
}

/**
 * ViewModel for the debt settlement bottom sheet.
 *
 * Lifecycle:
 * 1. On creation: loads open debts for the person via [DebtRepository.getOpenDebtsForPerson].
 * 2. As user inputs an amount: recomputes [SettlementUiState.Preview] live.
 * 3. On confirm: runs [SettleDebtsUseCase] and transitions to [SettlementUiState.Success].
 *
 * Toast events are emitted via [toastEvent] as a one-shot [SharedFlow] immediately after a
 * successful settlement. This is independent of [uiState] so the sheet can show a Toast while
 * closing — no double-booking guard needed.
 *
 * @param personId    The ID of the person whose debts are being settled.
 * @param isOwedToMe  Which direction of debts to settle.
 */
@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val settleDebtsUseCase: SettleDebtsUseCase,
    private val debtRepository: DebtRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettlementUiState>(
        SettlementUiState.Idle(totalOpen = 0.0, openCount = 0)
    )
    val uiState: StateFlow<SettlementUiState> = _uiState.asStateFlow()

    private val _openDebts = MutableStateFlow<List<com.buglist.domain.model.DebtEntryWithPayments>>(emptyList())
    val openDebts: StateFlow<List<com.buglist.domain.model.DebtEntryWithPayments>> = _openDebts.asStateFlow()

    /**
     * One-shot event emitted immediately after a successful settlement.
     * The Sheet collects this independently of [uiState] to show a Toast confirmation.
     * Emitted BEFORE [SettlementUiState.Success] so the Toast fires while the sheet is still
     * visible and then the sheet closes.
     * replay=0 ensures each event is delivered exactly once (no stale replay on recomposition).
     */
    private val _toastEvent = MutableSharedFlow<Pair<Double, Long>>(extraBufferCapacity = 1)
    val toastEvent: SharedFlow<Pair<Double, Long>> = _toastEvent.asSharedFlow()

    // Cached list of open debts with their remaining amounts — loaded once per sheet open
    private var openDebtsCache: List<com.buglist.domain.model.DebtEntryWithPayments> = emptyList()

    /**
     * Loads the open debts for the given person and direction.
     * Must be called when the sheet is shown, before any user interaction.
     */
    fun loadOpenDebts(personId: Long, isOwedToMe: Boolean) {
        viewModelScope.launch {
            try {
                openDebtsCache = debtRepository.getOpenDebtsForPerson(personId, isOwedToMe)
                    .map { dwp ->
                        val tagNames = tagRepository.getTagsForDebtEntry(dwp.entry.id)
                            .map { it.name }
                        dwp.copy(entry = dwp.entry.copy(tags = tagNames))
                    }
                _openDebts.value = openDebtsCache
                val totalOpen = openDebtsCache.sumOf { it.remaining }
                _uiState.value = SettlementUiState.Idle(
                    totalOpen = totalOpen,
                    openCount = openDebtsCache.size
                )
            } catch (e: Exception) {
                _uiState.value = SettlementUiState.Error("Fehler beim Laden: ${e.message}")
            }
        }
    }

    /**
     * Recomputes the settlement preview for the given input amount.
     * Called on every AmountInputPad change — must be fast (pure computation, no DB).
     */
    fun updatePreview(inputAmount: Double) {
        val currentState = _uiState.value
        val totalOpen = when (currentState) {
            is SettlementUiState.Idle -> currentState.totalOpen
            is SettlementUiState.Preview -> currentState.totalOpen
            else -> openDebtsCache.sumOf { it.remaining }
        }
        val openCount = openDebtsCache.size

        if (inputAmount <= 0.0) {
            _uiState.value = SettlementUiState.Idle(totalOpen = totalOpen, openCount = openCount)
            return
        }

        val preview = buildPreview(inputAmount)
        _uiState.value = SettlementUiState.Preview(
            inputAmount = inputAmount,
            totalOpen = totalOpen,
            openCount = openCount,
            preview = preview
        )
    }

    /**
     * Executes the settlement transaction for the given amount and direction.
     *
     * On success:
     * 1. Emits a [toastEvent] with the settled amount and timestamp — shown as a Toast
     *    confirmation while the sheet is still closing.
     * 2. Transitions to [SettlementUiState.Success] which triggers the sheet to close.
     *
     * The next press of TILGEN opens a fresh sheet (new [loadOpenDebts] call) and shows
     * no Toast — no double-booking guard required.
     */
    fun settleDebts(personId: Long, totalAmount: Double, isOwedToMe: Boolean) {
        viewModelScope.launch {
            _uiState.value = SettlementUiState.Processing
            when (val result = settleDebtsUseCase(personId, totalAmount, isOwedToMe)) {
                is Result.Success -> {
                    // Emit toast first — sheet will still render briefly while closing
                    _toastEvent.emit(Pair(totalAmount, System.currentTimeMillis()))
                    _uiState.value = SettlementUiState.Success(result.data)
                }
                is Result.Error -> _uiState.value = SettlementUiState.Error(result.message)
            }
        }
    }

    /**
     * Resets the ViewModel to Idle state — called when sheet is dismissed or after success.
     */
    fun reset(personId: Long, isOwedToMe: Boolean) {
        loadOpenDebts(personId, isOwedToMe)
    }

    /**
     * Pure FIFO allocation: distributes [inputAmount] across [openDebtsCache] and returns
     * a list of [SettlementPreviewItem]s reflecting the outcome.
     */
    private fun buildPreview(inputAmount: Double): List<SettlementPreviewItem> {
        var remainingBudget = inputAmount
        return openDebtsCache.map { debtWithPayments ->
            val debtRemaining = debtWithPayments.remaining
            if (remainingBudget <= 0.001 || debtRemaining <= 0.001) {
                // Not touched
                SettlementPreviewItem(
                    entry = debtWithPayments.entry,
                    currentRemaining = debtRemaining,
                    amountToPay = 0.0,
                    resultingStatus = debtWithPayments.entry.status,
                    isTouched = false
                )
            } else {
                val amountToApply = minOf(remainingBudget, debtRemaining)
                val newRemaining = debtRemaining - amountToApply
                val newStatus = if (newRemaining < 0.001) DebtStatus.PAID else DebtStatus.PARTIAL
                remainingBudget -= amountToApply
                SettlementPreviewItem(
                    entry = debtWithPayments.entry,
                    currentRemaining = debtRemaining,
                    amountToPay = amountToApply,
                    resultingStatus = newStatus,
                    isTouched = true
                )
            }
        }
    }
}
