package com.buglist.presentation.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Result
import com.buglist.domain.model.SettlementResult
import com.buglist.domain.usecase.AddDebtUseCase
import com.buglist.domain.usecase.SettleDebtsUseCase
import com.buglist.domain.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
 * Completion is delivered through [completedSettlements] without replay, independently
 * of the persistent screen state. Reopening the sheet never repeats a success callback.
 */
@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val settleDebtsUseCase: SettleDebtsUseCase,
    private val addDebtUseCase: AddDebtUseCase,
    private val debtRepository: DebtRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettlementUiState>(
        SettlementUiState.Idle(totalOpen = 0.0, openCount = 0)
    )
    val uiState: StateFlow<SettlementUiState> = _uiState.asStateFlow()

    private val _openDebts = MutableStateFlow<List<com.buglist.domain.model.DebtEntryWithPayments>>(emptyList())
    val openDebts: StateFlow<List<com.buglist.domain.model.DebtEntryWithPayments>> = _openDebts.asStateFlow()

    /** Completion is an event, never replayed when the same sheet opens again. */
    private val _completedSettlements = MutableSharedFlow<SettlementResult>(extraBufferCapacity = 1)
    val completedSettlements: SharedFlow<SettlementResult> = _completedSettlements.asSharedFlow()
    private var loadJob: Job? = null

    // Cached list of open debts with their remaining amounts — loaded once per sheet open
    private var openDebtsCache: List<com.buglist.domain.model.DebtEntryWithPayments> = emptyList()

    /**
     * Loads the open debts for the given person and direction.
     * Must be called when the sheet is shown, before any user interaction.
     */
    fun loadOpenDebts(personId: Long, isOwedToMe: Boolean) {
        if (_uiState.value is SettlementUiState.Processing) return
        loadJob?.cancel()
        openDebtsCache = emptyList()
        _openDebts.value = emptyList()
        // Reset synchronously, before the repository suspends. A previous Success
        // must never be mistaken for a new payment when the sheet reopens.
        _uiState.value = SettlementUiState.Idle(totalOpen = 0.0, openCount = 0)
        loadJob = viewModelScope.launch {
            try {
                openDebtsCache = debtRepository.getOpenDebtsForPerson(personId, isOwedToMe)
                _openDebts.value = openDebtsCache
                _uiState.value = SettlementUiState.Idle(
                    totalOpen = openDebtsCache.sumOf { it.remaining },
                    openCount = openDebtsCache.size
                )
            } catch (e: CancellationException) {
                throw e
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
        if (currentState !is SettlementUiState.Idle && currentState !is SettlementUiState.Preview) return
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

    /** One active booking at a time, even if confirm is tapped twice rapidly. */
    private fun beginSettlement(amount: Double): Boolean {
        val current = _uiState.value
        if (current !is SettlementUiState.Idle && current !is SettlementUiState.Preview) return false
        if (!amount.isFinite() || amount <= 0 || openDebtsCache.isEmpty()) return false
        return _uiState.compareAndSet(current, SettlementUiState.Processing)
    }

    private fun runSettlement(block: suspend () -> Result<SettlementResult>) {
        viewModelScope.launch {
            try {
                when (val result = block()) {
                    is Result.Success -> {
                        if (result.data.totalSettled <= 0.001) {
                            _uiState.value = SettlementUiState.Error("Es wurde kein Betrag getilgt.")
                        } else {
                            _uiState.value = SettlementUiState.Success(result.data)
                            _completedSettlements.emit(result.data)
                        }
                    }
                    is Result.Error -> _uiState.value = SettlementUiState.Error(result.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = SettlementUiState.Error("Tilgung fehlgeschlagen: ${e.message}")
            }
        }
    }

    fun settleDebts(personId: Long, totalAmount: Double, isOwedToMe: Boolean) {
        if (!beginSettlement(totalAmount)) return
        runSettlement { settleDebtsUseCase(personId, totalAmount, isOwedToMe) }
    }

    /** Settles the open balance, then records an overpayment in the opposite direction. */
    fun settleDebtsWithCounterDebt(
        personId: Long,
        totalAmount: Double,
        isOwedToMe: Boolean,
        currency: String
    ) {
        if (!beginSettlement(totalAmount)) return
        val totalOpen = openDebtsCache.sumOf { it.remaining }
        val overpayAmount = totalAmount - totalOpen
        runSettlement {
            when (val settlement = settleDebtsUseCase(personId, totalOpen, isOwedToMe)) {
                is Result.Success -> {
                    val counterDebt = DebtEntry(
                        personId = personId,
                        amount = overpayAmount,
                        currency = currency,
                        isOwedToMe = !isOwedToMe,
                        description = "Tilgungsüberschuss",
                        date = System.currentTimeMillis(),
                        status = DebtStatus.OPEN
                    )
                    when (val counter = addDebtUseCase(counterDebt)) {
                        is Result.Success -> settlement
                        is Result.Error -> Result.Error("Gegenschuld konnte nicht angelegt werden: ${counter.message}")
                    }
                }
                is Result.Error -> settlement
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
