package com.buglist.presentation.dashboard

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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
import com.buglist.util.appDataStore
import kotlinx.coroutines.Dispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
        /** Filtered items — result of search query (same as [items] when query empty). */
        val filteredItems: List<DashboardListItem>,
        /** Persons only — used for financial summary totals (includes inactive). */
        val persons: List<PersonWithBalance>,
        val totalBalance: Double,
        val totalOwedToMe: Double,
        val totalIOwe: Double,
        /** Current search query. Empty string = no filter active. */
        val searchQuery: String = "",
        /** Persons with openCount == 0, shown in collapsible section when [collapseInactivePersons] is true. */
        val inactivePersonItems: List<DashboardListItem.PersonItem> = emptyList(),
        /** Whether the inactive section is currently expanded. */
        val isInactiveExpanded: Boolean = false,
        /** Mirror of the DataStore setting — when false the inactive section is not shown. */
        val collapseInactivePersons: Boolean = true
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
    @ApplicationContext private val context: Context,
    getPersonsWithBalancesUseCase: GetPersonsWithBalancesUseCase,
    calculateTotalBalanceUseCase: CalculateTotalBalanceUseCase,
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val tagRepository: TagRepository,
    private val personRepository: PersonRepository,
    private val dividerRepository: DividerRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(PersonSortOrder.MANUAL)

    /** Feature B: Schnell-Suche — live filter for the crew list. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val KEY_COLLAPSE_INACTIVE = booleanPreferencesKey("collapse_inactive_persons")

    /** Reflects the DataStore preference for collapsing inactive persons. Default: true. */
    private val collapseInactiveFlow: Flow<Boolean> =
        context.appDataStore.data.map { it[KEY_COLLAPSE_INACTIVE] ?: true }

    /** Whether the inactive section is currently expanded in the UI. */
    private val _isInactiveExpanded = MutableStateFlow(false)

    val uiState: StateFlow<DashboardUiState> = _sortOrder.flatMapLatest { sortOrder ->
        combine(
            getPersonsWithBalancesUseCase(sortOrder),
            calculateTotalBalanceUseCase(),
            dividerRepository.getAllDividers(),
            _searchQuery,
            combine(collapseInactiveFlow, _isInactiveExpanded) { collapse, expanded -> collapse to expanded }
        ) { persons, totalBalance, dividers, query, (collapseInactive, isExpanded) ->

            // FULL list — ALL persons + dividers, sorted by sortIndex. Used by EDIT
            // MODE so the user can position every person (incl. debt-free ones);
            // each sortIndex is persisted. A debt-free person keeps their slot and
            // re-appears in the main list at that position once they get a debt.
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

            // Normal-mode display split: pull inactive persons (openCount == 0) out
            // of the main list into the collapsible section. Order preserved via
            // sortIndex — this is display-only and never touches the saved order.
            val mainItems: List<DashboardListItem>
            val inactiveItems: List<DashboardListItem.PersonItem>
            if (collapseInactive) {
                mainItems = items.filter {
                    it !is DashboardListItem.PersonItem || it.data.openCount > 0
                }
                inactiveItems = items.filterIsInstance<DashboardListItem.PersonItem>()
                    .filter { it.data.openCount == 0 }
            } else {
                mainItems = items
                inactiveItems = emptyList()
            }

            // Feature B: filter by search query (case-insensitive, persons only).
            // Applied to mainItems — the inactive section is hidden during search.
            val filteredItems: List<DashboardListItem> = if (query.isBlank()) {
                mainItems
            } else {
                mainItems.filter { item ->
                    when (item) {
                        is DashboardListItem.PersonItem ->
                            item.data.person.name.contains(query, ignoreCase = true)
                        is DashboardListItem.DividerItem -> false
                    }
                }
            }

            // Financial totals always use ALL persons (including inactive) for correctness
            val owedToMe = persons.filter { it.netBalance > 0 }.sumOf { it.netBalance }
            val iOwe = persons.filter { it.netBalance < 0 }.sumOf { -it.netBalance }
            DashboardUiState.Ready(
                items = items,
                filteredItems = filteredItems,
                persons = persons,
                totalBalance = totalBalance,
                totalOwedToMe = owedToMe,
                totalIOwe = iOwe,
                searchQuery = query,
                inactivePersonItems = inactiveItems,
                isInactiveExpanded = isExpanded,
                collapseInactivePersons = collapseInactive
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
     * Feature B: Updates the live search query that filters the crew list.
     *
     * @param query Search text. Empty string clears the filter.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Toggles whether the "Inaktiv" section at the bottom of the crew list is expanded.
     *
     * State is kept in-memory (not persisted) — resets to collapsed on every app start.
     */
    fun toggleInactiveExpanded() {
        _isInactiveExpanded.value = !_isInactiveExpanded.value
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
