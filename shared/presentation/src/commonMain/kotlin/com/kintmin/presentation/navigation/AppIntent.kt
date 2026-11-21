package com.kintmin.presentation.navigation

sealed interface AppIntent {
    data object InitializeMediaController : AppIntent
    data class HandleDeepLink(val url: String) : AppIntent
    data class ImportSharedAudio(val fileIdentifiers: List<String>) : AppIntent
    data object OnAppBackground : AppIntent
}
