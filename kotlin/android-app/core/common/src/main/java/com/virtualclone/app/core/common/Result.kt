package com.virtualclone.app.core.common

/**
 * A generic class that holds a value or an error.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * Returns the encapsulated result of the given transform function applied to the encapsulated value
 * if this instance represents success or the original encapsulated error if it is failure.
 */
inline fun <T, R> Result<T>.map(transform: (value: T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> Result.Error(exception)
        is Result.Loading -> Result.Loading
    }
}

/**
 * Returns the encapsulated result of the given transform function applied to the encapsulated value
 * if this instance represents success or the original encapsulated error if it is failure.
 */
inline fun <T, R> Result<T>.flatMap(transform: (value: T) -> Result<R>): Result<R> {
    return when (this) {
        is Result.Success -> transform(data)
        is Result.Error -> Result.Error(exception)
        is Result.Loading -> Result.Loading
    }
}

/**
 * Returns the encapsulated value if this instance represents success or the
 * result of the given function if it is failure.
 */
inline fun <T> Result<T>.getOrElse(defaultValue: () -> T): T {
    return when (this) {
        is Result.Success -> data
        is Result.Error -> defaultValue()
        is Result.Loading -> defaultValue()
    }
}

/**
 * Returns true if this instance represents a successful outcome.
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * Returns true if this instance represents a failure.
 */
fun <T> Result<T>.isError(): Boolean = this is Result.Error

/**
 * Returns true if this instance represents loading state.
 */
fun <T> Result<T>.isLoading(): Boolean = this is Result.Loading
