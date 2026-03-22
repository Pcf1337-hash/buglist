package com.buglist.presentation.add_divider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.domain.model.Divider
import com.buglist.domain.model.DividerLineStyle
import com.buglist.domain.repository.DividerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the [AddDividerSheet].
 */
sealed class AddDividerUiState {
    object Idle    : AddDividerUiState()
    object Loading : AddDividerUiState()
    data class Success(val dividerId: Long) : AddDividerUiState()
    data class Error(val message: String)   : AddDividerUiState()
}

/**
 * ViewModel for [AddDividerSheet].
 *
 * Validates input and persists the new [Divider] via [DividerRepository].
 * Uses StateFlow — never mutableStateOf.
 */
@HiltViewModel
class AddDividerViewModel @Inject constructor(
    private val dividerRepository: DividerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddDividerUiState>(AddDividerUiState.Idle)
    val uiState: StateFlow<AddDividerUiState> = _uiState.asStateFlow()

    /**
     * Saves a new divider with the given properties.
     *
     * @param label     Text shown on the divider line (may be blank for line-only separators).
     * @param color     ARGB color int for line and label.
     * @param lineStyle Visual line style.
     */
    fun saveDivider(label: String, color: Int, lineStyle: DividerLineStyle) {
        _uiState.value = AddDividerUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val id = dividerRepository.addDivider(
                    Divider(
                        label = label.trim(),
                        color = color,
                        lineStyle = lineStyle
                    )
                )
                _uiState.value = AddDividerUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = AddDividerUiState.Error(e.message ?: "Fehler beim Speichern")
            }
        }
    }

    /**
     * Updates an existing divider in place.
     *
     * @param id        Primary key of the [Divider] to update.
     * @param label     New label text.
     * @param color     New ARGB color int.
     * @param lineStyle New visual line style.
     * @param sortIndex Preserved sort position (unchanged by edit).
     */
    fun updateDivider(
        id: Long,
        label: String,
        color: Int,
        lineStyle: DividerLineStyle,
        sortIndex: Int
    ) {
        _uiState.value = AddDividerUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dividerRepository.updateDivider(
                    Divider(
                        id = id,
                        label = label.trim(),
                        color = color,
                        lineStyle = lineStyle,
                        sortIndex = sortIndex
                    )
                )
                _uiState.value = AddDividerUiState.Success(id)
            } catch (e: Exception) {
                _uiState.value = AddDividerUiState.Error(e.message ?: "Fehler beim Speichern")
            }
        }
    }

    /** Resets state back to [AddDividerUiState.Idle] after the sheet has been handled. */
    fun reset() {
        _uiState.value = AddDividerUiState.Idle
    }
}
