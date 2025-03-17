package com.kintmin.ytmusicbox.ui.sample

object YoutubeDownloadConstants {
    sealed interface State {
        data object Idle: State
        data object Loading: State
        data class Success(val id: String): State
        data class Failed(val errorMessage: String): State
    }
}