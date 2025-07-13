package com.kintmin.presentation.ui.main.youtube_search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.platform.worker.usecase.ExecuteYoutubeDownload
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YoutubeDownloadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val executeYoutubeDownload: ExecuteYoutubeDownload,
) : ViewModel() {

    private val _currentUrl = MutableStateFlow(
        savedStateHandle.toRoute<MainScreenRoute>().searchUrl ?: "https://www.youtube.com/"
    )
    val currentUrl = _currentUrl.asStateFlow()

    private val _eventFlow = MutableSharedFlow<YoutubeWebViewEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _clickedUrlSet = mutableSetOf<String>()

    fun sendIntent(intent: YoutubeDownloadIntent) {
        when(intent) {
            is YoutubeDownloadIntent.OnClickDownload -> startDownload(intent.url)
            is YoutubeDownloadIntent.OnChangeUrl -> _currentUrl.update { intent.url }
        }
    }

    private fun startDownload(youtubeUrl: String) {
        if (_clickedUrlSet.contains(youtubeUrl)) {
            viewModelScope.launch {
                _eventFlow.emit(YoutubeWebViewEvent.ShowToast("저장중이거나 저장완료된 영상입니다."))
            }
            return
        }

        _clickedUrlSet.add(youtubeUrl)
        executeYoutubeDownload(youtubeUrl)
    }
}