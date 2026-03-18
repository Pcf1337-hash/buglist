package com.buglist.presentation.person_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.domain.model.DebtEntryWithPayments
import com.buglist.domain.model.DebtStatus
import com.buglist.domain.model.Person
import com.buglist.domain.repository.TagRepository
import com.buglist.domain.usecase.CancelDebtUseCase
import com.buglist.domain.usecase.DeletePersonUseCase
import com.buglist.domain.usecase.MarkDebtAsPaidUseCase
import com.buglist.domain.repository.DebtRepository
import com.buglist.domain.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tab selection for the person detail debt list.
 */
enum class DebtTab { OPEN, PAID, ALL }

/**
 * UI state for the person detail screen.
 */
sealed class PersonDetailUiState {
    object Loading : PersonDetailUiState()
    data class Ready(
        val person: Person,
        val debts: List<DebtEntryWithPayments>,
        val activeTab: DebtTab,
        val expandedDebtId: Long?,
        /** true if there are OPEN or PARTIAL entries where the person owes the user. */
        val hasOpenDebtsOwedToMe: Boolean,
        /** true if there are OPEN or PARTIAL entries where the user owes the person. */
        val hasOpenDebtsIOwe: Boolean
    ) : PersonDetailUiState()
    object PersonNotFound : PersonDetailUiState()
}

/**
 * ViewModel for the person detail screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personRepository: PersonRepository,
    private val debtRepository: DebtRepository,
    private val tagRepository: TagRepository,
    private val markDebtAsPaidUseCase: MarkDebtAsPaidUseCase,
    private val cancelDebtUseCase: CancelDebtUseCase,
    private val deletePersonUseCase: DeletePersonUseCase
) : ViewModel() {

    val personId: Long = checkNotNull(savedStateHandle["personId"])

    private val _activeTab = MutableStateFlow(DebtTab.OPEN)
    val activeTab: StateFlow<DebtTab> = _activeTab.asStateFlow()

    private val _expandedDebtId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<PersonDetailUiState> = combine(
        personRepository.getPersonById(personId),
        _activeTab,
        _expandedDebtId
    ) { person, tab, expandedId ->
        Triple(person, tab, expandedId)
    }.flatMapLatest { (person, tab, expandedId) ->
        if (person == null) {
            flowOf(PersonDetailUiState.PersonNotFound)
        } else {
            val statuses = when (tab) {
                DebtTab.OPEN -> listOf(DebtStatus.OPEN, DebtStatus.PARTIAL)
                DebtTab.PAID -> listOf(DebtStatus.PAID, DebtStatus.CANCELLED)
                DebtTab.ALL -> DebtStatus.entries
            }
            // Load ALL debts (no status filter) to compute settlement availability.
            // transformLatest allows calling suspend functions (tag loading) for each entry.
            // Explicit type annotation needed to unify both if/else branches in flatMapLatest.
            debtRepository.getDebtEntriesWithPaymentsForPerson(personId).transformLatest<List<DebtEntryWithPayments>, PersonDetailUiState> { allDebts ->
                val openStatuses = setOf(DebtStatus.OPEN, DebtStatus.PARTIAL)
                val filtered = allDebts.filter { it.entry.status in statuses }
                val hasOpenOwedToMe = allDebts.any {
                    it.entry.isOwedToMe && it.entry.status in openStatuses
                }
                val hasOpenIOwe = allDebts.any {
                    !it.entry.isOwedToMe && it.entry.status in openStatuses
                }
                // Enrich filtered entries with their tags (suspend call per entry).
                val enriched = filtered.map { dwp ->
                    val tagNames = tagRepository.getTagsForDebtEntry(dwp.entry.id)
                        .map { it.name }
                    dwp.copy(entry = dwp.entry.copy(tags = tagNames))
                }
                emit(
                    PersonDetailUiState.Ready(
                        person = person,
                        debts = enriched,
                        activeTab = tab,
                        expandedDebtId = expandedId,
                        hasOpenDebtsOwedToMe = hasOpenOwedToMe,
                        hasOpenDebtsIOwe = hasOpenIOwe
                    )
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PersonDetailUiState.Loading
    )

    fun setActiveTab(tab: DebtTab) {
        _activeTab.value = tab
    }

    fun toggleDebtExpanded(debtId: Long) {
        _expandedDebtId.value = if (_expandedDebtId.value == debtId) null else debtId
    }

    fun markAsPaid(debtId: Long) {
        viewModelScope.launch {
            markDebtAsPaidUseCase(debtId)
        }
    }

    /** Maps debtId → status that was active before cancellation, for undo. */
    private val cancelUndoMap = mutableMapOf<Long, DebtStatus>()

    fun cancelDebt(debtId: Long) {
        viewModelScope.launch {
            // Snapshot current status before cancelling for potential undo
            val currentState = uiState.value
            if (currentState is PersonDetailUiState.Ready) {
                val debt = currentState.debts.firstOrNull { it.entry.id == debtId }
                if (debt != null) {
                    cancelUndoMap[debtId] = debt.entry.status
                }
            }
            cancelDebtUseCase(debtId)
        }
    }

    /**
     * Reverts a cancel action by restoring the previous status.
     * Called when user taps "RÜCKGÄNGIG" in the snackbar.
     */
    fun undoCancelDebt(debtId: Long) {
        viewModelScope.launch {
            val previousStatus = cancelUndoMap.remove(debtId) ?: DebtStatus.OPEN
            debtRepository.updateDebtStatus(debtId, previousStatus)
        }
    }

    fun deletePerson() {
        viewModelScope.launch {
            val state = uiState.value
            if (state is PersonDetailUiState.Ready) {
                deletePersonUseCase(state.person)
            }
        }
    }
}
