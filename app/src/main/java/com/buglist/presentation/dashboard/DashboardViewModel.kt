package com.buglist.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.data.remote.UpdateState
import com.buglist.domain.model.PersonWithBalance
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
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(PersonSortOrder.NAME)
    val sortOrder: StateFlow<PersonSortOrder> = _sortOrder.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = _sortOrder.flatMapLatest { sortOrder ->
        combine(
            getPersonsWithBalancesUseCase(sortOrder),
            calculateTotalBalanceUseCase()
        ) { persons, totalBalance ->
            val owedToMe = persons.filter { it.netBalance > 0 }.sumOf { it.netBalance }
            val iOwe = persons.filter { it.netBalance < 0 }.sumOf { -it.netBalance }
            DashboardUiState.Ready(
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

    fun setSortOrder(order: PersonSortOrder) {
        _sortOrder.value = order
    }
}
