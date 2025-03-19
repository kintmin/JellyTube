package com.kintmin.presentation.ui

import com.kintmin.domain.model.AudioMediaData

object YoutubeDownloadConstants {
    sealed interface State {
        data object Idle: State
        data object Loading: State
        data class Success(val audioMediaData: AudioMediaData): State
        data class Failed(val errorMessage: String): State
    }
}