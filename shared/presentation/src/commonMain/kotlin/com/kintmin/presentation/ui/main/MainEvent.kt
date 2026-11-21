package com.kintmin.presentation.ui.main

sealed interface MainEvent {
    data class ShowToast(val message: String) : MainEvent
}
