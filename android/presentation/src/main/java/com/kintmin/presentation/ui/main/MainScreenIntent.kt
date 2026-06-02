package com.kintmin.presentation.ui.main

sealed interface MainScreenIntent {
    data class ChangeTab(val tab: MainTabItem) : MainScreenIntent
    data class ImportMediaFiles(val uriStrings: List<String>) : MainScreenIntent
}