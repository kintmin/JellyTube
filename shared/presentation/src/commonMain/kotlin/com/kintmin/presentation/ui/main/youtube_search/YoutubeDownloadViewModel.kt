package com.kintmin.presentation.ui.main.youtube_search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.presentation.port.StartYoutubeDownload
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import com.kintmin.presentation.ui.main.navigation.mainScreenTypeMap
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class YoutubeDownloadViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val executeYoutubeDownload: StartYoutubeDownload,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        YoutubeDownloadUiState(
            currentUrl = savedStateHandle.toRoute<MainScreenRoute>(mainScreenTypeMap).searchUrl ?: "https://m.youtube.com/"
        )
    )
    val uiState: StateFlow<YoutubeDownloadUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<YoutubeDownloadEvent>(Channel.BUFFERED)
    val eventFlow = eventChannel.receiveAsFlow()

    private val _clickedUrlSet = mutableSetOf<String>()

    fun sendIntent(intent: YoutubeDownloadIntent) {
        when(intent) {
            is YoutubeDownloadIntent.OnClickDownload -> startDownload(intent.url)
            is YoutubeDownloadIntent.OnChangeUrl -> _uiState.update { it.copy(currentUrl = intent.url) }
            is YoutubeDownloadIntent.OnShowToast -> viewModelScope.launch {
                eventChannel.send(YoutubeDownloadEvent.ShowToast(intent.message))
            }
        }
    }

    private fun startDownload(youtubeUrl: String) {
        if (_clickedUrlSet.contains(youtubeUrl)) {
            viewModelScope.launch {
                eventChannel.send(YoutubeDownloadEvent.ShowToast("저장중이거나 저장완료된 영상입니다."))
            }
            return
        }

        _clickedUrlSet.add(youtubeUrl)
        executeYoutubeDownload(youtubeUrl)
    }
}
