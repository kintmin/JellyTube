package com.kintmin.presentation.ui.main

sealed interface MainIntent {
    data class ChangeTab(val tab: MainTabItem) : MainIntent
    data class CheckAppUpdate(val currentVersionName: String) : MainIntent
    data object IgnoreAppUpdate : MainIntent
    data object DismissAppUpdate : MainIntent
}
