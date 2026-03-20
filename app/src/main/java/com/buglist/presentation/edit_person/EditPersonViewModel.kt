package com.buglist.presentation.edit_person

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buglist.domain.model.Person
import com.buglist.domain.model.Result
import com.buglist.domain.usecase.UpdatePersonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed class EditPersonUiState {
    object Idle : EditPersonUiState()
    object Loading : EditPersonUiState()
    object Success : EditPersonUiState()
    data class Error(val message: String) : EditPersonUiState()
}

/**
 * ViewModel for the edit-person bottom sheet.
 *
 * Handles persisting name/phone/notes changes as well as the optional
 * custom avatar photo workflow:
 *  1. User picks an image from the system photo picker (URI provided by the sheet).
 *  2. [copyImageToInternal] copies + compresses the image to internal storage.
 *  3. The returned path is stored on the [Person] record via [UpdatePersonUseCase].
 *
 * Image files are stored at `<filesDir>/avatars/person_<id>.jpg` and survive
 * app updates. They are NOT deleted when the person is deleted (small footprint —
 * cleanup can be added as a maintenance task later).
 */
@HiltViewModel
class EditPersonViewModel @Inject constructor(
    private val updatePersonUseCase: UpdatePersonUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditPersonUiState>(EditPersonUiState.Idle)
    val uiState: StateFlow<EditPersonUiState> = _uiState.asStateFlow()

    /**
     * Persists all changes to a person record.
     *
     * @param person          The original [Person] domain object (supplies id, createdAt, etc.).
     * @param name            Updated display name.
     * @param phone           Updated phone number (blank → null).
     * @param notes           Updated notes (blank → null).
     * @param avatarColor     Selected avatar background color (0 = auto from name).
     * @param avatarImagePath Path to the custom photo, or null to keep the initials avatar.
     */
    fun savePerson(
        person: Person,
        name: String,
        phone: String,
        notes: String,
        avatarColor: Int,
        avatarImagePath: String?
    ) {
        _uiState.value = EditPersonUiState.Loading
        viewModelScope.launch {
            val result = updatePersonUseCase(
                person.copy(
                    name = name.trim(),
                    phone = phone.ifBlank { null },
                    notes = notes.ifBlank { null },
                    avatarColor = avatarColor,
                    avatarImagePath = avatarImagePath
                )
            )
            _uiState.value = when (result) {
                is Result.Success -> EditPersonUiState.Success
                is Result.Error   -> EditPersonUiState.Error(result.message)
            }
        }
    }

    /**
     * Copies the image at [uri] into `<filesDir>/avatars/person_<personId>.jpg`,
     * scaling it down to at most 512×512 pixels and compressing to 85 % JPEG quality.
     *
     * Must be called from a coroutine — runs on [Dispatchers.IO].
     *
     * @return The absolute path of the saved file, or `null` if the operation failed.
     */
    suspend fun copyImageToInternal(personId: Long, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "avatars").apply { mkdirs() }
                val destFile = File(dir, "person_${personId}.jpg")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    val original = BitmapFactory.decodeStream(input) ?: return@withContext null
                    val scaled = scaleBitmap(original, maxDim = 512)
                    destFile.outputStream().use { out ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    if (scaled !== original) scaled.recycle()
                    original.recycle()
                    destFile.absolutePath
                }
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Deletes the avatar image file for [personId] from internal storage.
     * Used when the user explicitly removes the custom photo.
     */
    fun deleteAvatarImage(personId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                File(context.filesDir, "avatars/person_${personId}.jpg").delete()
            } catch (_: Exception) { /* silently ignored */ }
        }
    }

    /** Scales [src] so neither dimension exceeds [maxDim], preserving aspect ratio. */
    private fun scaleBitmap(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val scale = maxDim.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(src, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    fun reset() {
        _uiState.value = EditPersonUiState.Idle
    }
}
