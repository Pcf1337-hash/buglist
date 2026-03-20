package com.buglist.presentation.add_debt

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.domain.model.DebtEntry
import com.buglist.domain.model.Payment
import com.buglist.domain.model.Result
import com.buglist.domain.model.Tag
import com.buglist.domain.repository.TagRepository
import com.buglist.domain.usecase.AddDebtUseCase
import com.buglist.domain.usecase.AddPartialPaymentUseCase
import com.buglist.domain.usecase.UpdateDebtEntryUseCase
import com.buglist.util.appDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val KEY_SHOW_DESCRIPTION = booleanPreferencesKey("show_description")

sealed class AddDebtUiState {
    object Idle : AddDebtUiState()
    object Loading : AddDebtUiState()
    object Success : AddDebtUiState()
    data class Error(val message: String) : AddDebtUiState()
}

sealed class AddPaymentUiState {
    object Idle : AddPaymentUiState()
    object Loading : AddPaymentUiState()
    object Success : AddPaymentUiState()
    data class Error(val message: String) : AddPaymentUiState()
    /**
     * Emitted when BUCHEN is pressed immediately after a successful payment without a new amount.
     * The UI should show a toast with the last payment details and NOT book again.
     */
    data class ShowLastPaymentToast(val amount: Double, val date: Long) : AddPaymentUiState()
}

/**
 * ViewModel for the add-debt and add-payment bottom sheets.
 *
 * Exposes [allTags] and [selectedTagIds] for the tag chip UI in [AddDebtSheet].
 * Tags are associated with debt entries via [TagRepository.setTagsForDebtEntry]
 * after a successful insert or update.
 */
@HiltViewModel
class AddDebtViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addDebtUseCase: AddDebtUseCase,
    private val addPartialPaymentUseCase: AddPartialPaymentUseCase,
    private val updateDebtEntryUseCase: UpdateDebtEntryUseCase,
    private val tagRepository: TagRepository
) : ViewModel() {

    /**
     * Reflects the "Kommentarfeld anzeigen" setting from DataStore.
     * When true, [AddDebtSheet] renders a description/comment text field.
     * Defaults to false (off) until the preference is loaded.
     */
    val showDescription: StateFlow<Boolean> = context.appDataStore.data
        .map { prefs -> prefs[KEY_SHOW_DESCRIPTION] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _debtUiState = MutableStateFlow<AddDebtUiState>(AddDebtUiState.Idle)
    val debtUiState: StateFlow<AddDebtUiState> = _debtUiState.asStateFlow()

    private val _paymentUiState = MutableStateFlow<AddPaymentUiState>(AddPaymentUiState.Idle)
    val paymentUiState: StateFlow<AddPaymentUiState> = _paymentUiState.asStateFlow()

    /** All available tags — observed from [TagRepository.getAllTags]. */
    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** IDs of the tags currently selected for the debt being created/edited. */
    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()

    /** Amount of the most recently booked payment — null if no payment made this session. */
    private val _lastPaymentAmount = MutableStateFlow<Double?>(null)
    val lastPaymentAmount: StateFlow<Double?> = _lastPaymentAmount.asStateFlow()

    /** Timestamp of the most recently booked payment. */
    private val _lastPaymentDate = MutableStateFlow<Long?>(null)
    val lastPaymentDate: StateFlow<Long?> = _lastPaymentDate.asStateFlow()

    /**
     * True after a successful payment until the user enters a new amount.
     * Used to prevent accidental double-booking via a second "BUCHEN" tap.
     */
    private val _hasJustPaid = MutableStateFlow(false)
    val hasJustPaid: StateFlow<Boolean> = _hasJustPaid.asStateFlow()

    /**
     * Called by the UI when the user modifies the amount input field.
     * Resets the just-paid guard so the next submit triggers a real payment.
     */
    fun onAmountInputChanged() {
        _hasJustPaid.value = false
    }

    /**
     * Toggles a tag selection on/off for the current debt being created or edited.
     *
     * @param tagId ID of the tag to toggle.
     */
    fun toggleTag(tagId: Long) {
        _selectedTagIds.value = if (tagId in _selectedTagIds.value) {
            _selectedTagIds.value - tagId
        } else {
            _selectedTagIds.value + tagId
        }
    }

    /**
     * Pre-selects the tags already attached to [existingEntry] when opening in edit mode.
     * Should be called once when the sheet is opened with a non-null existing debt.
     *
     * @param existingEntry The debt entry being edited.
     */
    fun loadTagsForExistingDebt(existingEntry: DebtEntry) {
        viewModelScope.launch {
            val tags = tagRepository.getTagsForDebtEntry(existingEntry.id)
            _selectedTagIds.value = tags.map { it.id }.toSet()
        }
    }

    /** Clears the selected tag set — called when the sheet resets. */
    fun clearSelectedTags() {
        _selectedTagIds.value = emptySet()
    }

    /**
     * Saves a new debt entry.
     *
     * @param personId     Person this debt belongs to.
     * @param amountString Raw input string from [AmountInputPad] (e.g., "123,45").
     * @param isOwedToMe   Direction toggle.
     * @param date         Debt date in ms.
     * @param dueDate      Optional due date in ms.
     * @param description  Optional description.
     * @param currency     Currency code.
     */
    fun saveDebt(
        personId: Long,
        amountString: String,
        isOwedToMe: Boolean,
        date: Long,
        dueDate: Long?,
        description: String?,
        currency: String = "EUR"
    ) {
        val amount = amountString.replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _debtUiState.value = AddDebtUiState.Error("Invalid amount")
            return
        }
        _debtUiState.value = AddDebtUiState.Loading
        viewModelScope.launch {
            val result = addDebtUseCase(
                DebtEntry(
                    personId = personId,
                    amount = amount,
                    currency = currency,
                    isOwedToMe = isOwedToMe,
                    description = description?.ifBlank { null },
                    date = date,
                    dueDate = dueDate
                )
            )
            _debtUiState.value = when (result) {
                is Result.Success -> {
                    // Persist selected tags for the newly created debt entry.
                    val newId = result.data
                    if (_selectedTagIds.value.isNotEmpty()) {
                        tagRepository.setTagsForDebtEntry(newId, _selectedTagIds.value.toList())
                    }
                    AddDebtUiState.Success
                }
                is Result.Error -> AddDebtUiState.Error(result.message)
            }
        }
    }

    /**
     * Records a partial payment on an existing debt.
     *
     * If [hasJustPaid] is true when this is called, the payment is skipped and
     * the state transitions to [AddPaymentUiState.ShowLastPaymentToast] so the UI
     * can display a reminder toast instead of double-booking.
     */
    fun savePayment(
        debtEntryId: Long,
        amountString: String,
        note: String?
    ) {
        if (_hasJustPaid.value) {
            _paymentUiState.value = AddPaymentUiState.ShowLastPaymentToast(
                amount = _lastPaymentAmount.value ?: 0.0,
                date = _lastPaymentDate.value ?: System.currentTimeMillis()
            )
            return
        }
        val amount = amountString.replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _paymentUiState.value = AddPaymentUiState.Error("Invalid amount")
            return
        }
        _paymentUiState.value = AddPaymentUiState.Loading
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val result = addPartialPaymentUseCase(
                Payment(
                    debtEntryId = debtEntryId,
                    amount = amount,
                    note = note?.ifBlank { null },
                    date = now
                )
            )
            _paymentUiState.value = when (result) {
                is Result.Success -> {
                    _lastPaymentAmount.value = amount
                    _lastPaymentDate.value = now
                    _hasJustPaid.value = true
                    AddPaymentUiState.Success
                }
                is Result.Error -> AddPaymentUiState.Error(result.message)
            }
        }
    }

    /**
     * Updates an existing debt entry.
     *
     * Preserves the entry's current status (OPEN/PARTIAL/PAID/CANCELLED) —
     * only amount, description, direction, currency, date, and dueDate change.
     *
     * @param existingEntry The original entry with its id and current status.
     * @param amountString  Raw input string from [AmountInputPad] (e.g., "123,45").
     * @param isOwedToMe    Updated direction toggle.
     * @param date          Updated debt date in ms.
     * @param dueDate       Updated optional due date in ms.
     * @param description   Updated optional description.
     * @param currency      Currency code.
     */
    fun updateDebt(
        existingEntry: DebtEntry,
        amountString: String,
        isOwedToMe: Boolean,
        date: Long,
        dueDate: Long?,
        description: String?,
        currency: String = "EUR"
    ) {
        val amount = amountString.replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _debtUiState.value = AddDebtUiState.Error("Invalid amount")
            return
        }
        _debtUiState.value = AddDebtUiState.Loading
        viewModelScope.launch {
            val updated = existingEntry.copy(
                amount = amount,
                currency = currency,
                isOwedToMe = isOwedToMe,
                description = description?.ifBlank { null },
                date = date,
                dueDate = dueDate
            )
            val result = updateDebtEntryUseCase(updated)
            _debtUiState.value = when (result) {
                is Result.Success -> {
                    // Update tags for the existing entry (replaces previous associations).
                    tagRepository.setTagsForDebtEntry(
                        existingEntry.id,
                        _selectedTagIds.value.toList()
                    )
                    AddDebtUiState.Success
                }
                is Result.Error -> AddDebtUiState.Error(result.message)
            }
        }
    }

    /** Resets debt state to Idle and clears selected tags. */
    fun resetDebtState() {
        _debtUiState.value = AddDebtUiState.Idle
        _selectedTagIds.value = emptySet()
    }

    /** Resets the payment state to Idle and clears the just-paid guard. */
    fun resetPaymentState() {
        _paymentUiState.value = AddPaymentUiState.Idle
        _hasJustPaid.value = false
    }
}
