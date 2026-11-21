package com.kintmin.presentation.ui.main

sealed interface MainIntent {
    data class ChangeTab(val tab: MainTabItem) : MainIntent
    data class ImportMediaFiles(val uriStrings: List<String>) : MainIntent
}