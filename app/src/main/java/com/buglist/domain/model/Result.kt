package com.buglist.domain.model

/**
 * Generic result wrapper for use-case operations.
 *
 * Replaces raw exceptions at the boundary between use cases and ViewModels.
 * All use cases return [Result] so the presentation layer never needs to catch
 * exceptions directly.
 *
 * Usage:
 * ```kotlin
 * when (val result = addDebtUseCase(params)) {
 *     is Result.Success -> navigateToDashboard(result.data)
 *     is Result.Error   -> showError(result.message)
 * }
 * ```
 */
sealed class Result<out T> {

    /**
     * The operation completed successfully.
     * @param data The returned value. Use [Unit] for write-only operations.
     */
    data class Success<out T>(val data: T) : Result<T>()

    /**
     * The operation failed.
     * @param message Human-readable error description (not shown directly in UI —
     *                map to string resources in the ViewModel).
     * @param cause   Optional underlying exception for logging.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : Result<Nothing>()

    /** True if this is a [Success]. */
    val isSuccess: Boolean get() = this is Success

    /** True if this is an [Error]. */
    val isError: Boolean get() = this is Error

    /**
     * Returns the [Success.data] value, or null if this is an [Error].
     */
    fun getOrNull(): T? = (this as? Success)?.data

    /**
     * Returns the [Success.data] value, or [default] if this is an [Error].
     */
    fun getOrDefault(default: @UnsafeVariance T): T =
        (this as? Success)?.data ?: default
}
