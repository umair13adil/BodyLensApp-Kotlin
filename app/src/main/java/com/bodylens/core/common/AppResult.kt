package com.bodylens.core.common

/** Lightweight result wrapper for the domain/data boundary. */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val cause: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    data object NoPersonDetected : AppError
    data object PoseNotUsable : AppError
    data object MissingHeight : AppError
    data class Unexpected(val message: String) : AppError
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
}
