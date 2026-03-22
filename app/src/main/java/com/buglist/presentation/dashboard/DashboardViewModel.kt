package com.buglist.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.data.remote.UpdateState
import com.buglist.domain.model.DashboardListItem
import com.buglist.domain.model.PersonWithBalance
import com.buglist.domain.repository.DividerRepository
import com.buglist.domain.repository.PersonRepository
import com.buglist.domain.repository.TagRepository
import com.buglist.domain.usecase.CalculateTotalBalanceUseCase
import com.buglist.domain.usecase.CheckForUpdateUseCase
import com.buglist.domain.usecase.GetPersonsWithBalancesUseCase
import com.buglist.domain.usecase.PersonSortOrder
import kotlinx.coroutines.Dispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the dashboard screen.
 */
sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Ready(
        /** Combined, sorted list of persons and dividers for the crew list. */
        val items: List<DashboardListItem>,
        /** Persons only — used for financial summary totals. */
        val persons: List<PersonWithBalance>,
        val totalBalance: Double,
        val totalOwedToMe: Double,
        val totalIOwe: Double
    ) : DashboardUiState()
}

/**
 * ViewModel for the dashboard screen.
 *
 * Combines persons-with-balances and total net balance into a single [DashboardUiState].
 * StateFlow is used — never mutableStateOf (see L-011 in lessons.md).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    getPersonsWithBalancesUseCase: GetPersonsWithBalancesUseCase,
    calculateTotalBalanceUseCase: CalculateTotalBalanceUseCase,
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val tagRepository: TagRepository,
    private val personRepository: PersonRepository,
    private val dividerRepository: DividerRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(PersonSortOrder.MANUAL)

    val uiState: StateFlow<DashboardUiState> = _sortOrder.flatMapLatest { sortOrder ->
        combine(
            getPersonsWithBalancesUseCase(sortOrder),
            calculateTotalBalanceUseCase(),
            dividerRepository.getAllDividers()
        ) { persons, totalBalance, dividers ->
            // Merge persons and dividers into one sorted list by sortIndex.
            // Persons come from the DB already sorted (sortIndex ASC, name ASC).
            // We re-sort the combined list so dividers are interleaved correctly.
            val items: List<DashboardListItem> = buildList {
                addAll(persons.map { DashboardListItem.PersonItem(it) })
                addAll(dividers.map { DashboardListItem.DividerItem(it) })
            }.sortedWith(
                compareBy({ it.sortIndex }, {
                    // Secondary: persons sort by name, dividers by label — keeps stable order
                    when (it) {
                        is DashboardListItem.PersonItem  -> it.data.person.name
                        is DashboardListItem.DividerItem -> it.data.label
                    }
                })
            )
            val owedToMe = persons.filter { it.netBalance > 0 }.sumOf { it.netBalance }
            val iOwe = persons.filter { it.netBalance < 0 }.sumOf { -it.netBalance }
            DashboardUiState.Ready(
                items = items,
                persons = persons,
                totalBalance = totalBalance,
                totalOwedToMe = owedToMe,
                totalIOwe = iOwe
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState.Loading
    )

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        // Seed default tags on first launch — safe here because DashboardViewModel
        // is only created after auth success + DB initialization.
        viewModelScope.launch(Dispatchers.IO) {
            tagRepository.insertDefaultTagsIfEmpty()
        }
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2_000)
            _updateState.value = checkForUpdateUseCase()
        }
    }

    fun onUpdateDismissed() {
        _updateState.value = UpdateState.Idle
    }

    fun onUpdateSkipped(version: String) {
        viewModelScope.launch {
            checkForUpdateUseCase.skipVersion(version)
            _updateState.value = UpdateState.Idle
        }
    }

    fun checkForUpdateManually() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            _updateState.value = checkForUpdateUseCase(forceCheck = true)
        }
    }

    /**
     * Persists the manual drag-to-reorder result for the combined crew list.
     *
     * Each item (person or divider) receives a [sortIndex] equal to its position
     * in the merged list (0, 1, 2, …). Both tables are updated so the next
     * combined query re-assembles the list in the user's chosen order.
     *
     * @param orderedItems Combined person+divider list in the new desired order.
     */
    fun saveOrder(orderedItems: List<DashboardListItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            orderedItems.forEachIndexed { index, item ->
                when (item) {
                    is DashboardListItem.PersonItem  ->
                        personRepository.updatePersonSortIndex(item.data.person.id, index)
                    is DashboardListItem.DividerItem ->
                        dividerRepository.updateSortIndex(item.data.id, index)
                }
            }
        }
    }

    /**
     * Deletes a divider separator from the crew list.
     *
     * @param dividerId The ID of the [com.buglist.domain.model.Divider] to remove.
     */
    fun deleteDivider(dividerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dividerRepository.deleteDivider(dividerId)
        }
    }
}
