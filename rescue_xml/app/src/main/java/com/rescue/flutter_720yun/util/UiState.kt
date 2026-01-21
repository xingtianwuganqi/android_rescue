package com.rescue.flutter_720yun.util

// UiState.kt
sealed class UiState<out T> {
    data object FirstLoading: UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
//    data object Empty : UiState<Nothing>()
    data class Error(val message: String?) : UiState<Nothing>()
}
