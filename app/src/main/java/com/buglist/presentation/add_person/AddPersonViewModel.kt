package com.buglist.presentation.add_person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.domain.model.Person
import com.buglist.domain.model.Result
import com.buglist.domain.usecase.AddPersonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddPersonUiState {
    object Idle : AddPersonUiState()
    object Loading : AddPersonUiState()
    data class Success(val personId: Long) : AddPersonUiState()
    data class Error(val message: String) : AddPersonUiState()
}

/**
 * ViewModel for the add-person bottom sheet.
 */
@HiltViewModel
class AddPersonViewModel @Inject constructor(
    private val addPersonUseCase: AddPersonUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddPersonUiState>(AddPersonUiState.Idle)
    val uiState: StateFlow<AddPersonUiState> = _uiState.asStateFlow()

    fun savePerson(name: String, phone: String, notes: String) {
        _uiState.value = AddPersonUiState.Loading
        viewModelScope.launch {
            val result = addPersonUseCase(
                Person(
                    name = name,
                    phone = phone.ifBlank { null },
                    notes = notes.ifBlank { null }
                )
            )
            _uiState.value = when (result) {
                is Result.Success -> AddPersonUiState.Success(result.data)
                is Result.Error -> AddPersonUiState.Error(result.message)
            }
        }
    }

    fun reset() { _uiState.value = AddPersonUiState.Idle }
}
