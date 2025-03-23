package com.kintmin.presentation.ui.common

sealed class UiState<out T> {
    data object OnLoading : UiState<Nothing>()
    data class OnSuccess<T>(val data: T) : UiState<T>()
    data class OnFail(val exception: Throwable) : UiState<Nothing>()
}
