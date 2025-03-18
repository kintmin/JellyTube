package com.kintmin.ytmusicbox.ui.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.ytmusicbox.data.local.LocalFileDataSource
import com.kintmin.ytmusicbox.data.local.YoutubeDownloadDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YoutubeDownloadViewModel @Inject constructor(
    private val youtubeDownloadDataSource: YoutubeDownloadDataSource,
    private val localFileDataSource: LocalFileDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow<YoutubeDownloadConstants.State>(YoutubeDownloadConstants.State.Idle)
    val state = _state.asStateFlow()

    fun startDownload(youtubeUrl: String) {
        viewModelScope.launch {
            _state.update { YoutubeDownloadConstants.State.Loading }

            val videoId = extractVideoId(youtubeUrl)
            if (videoId == null) {
                println("유튜브 url 형식이 아닙니다.")
                return@launch
            }

            val isExistMediaFile = localFileDataSource.getYoutubeData(videoId).getOrNull() != null
            if (isExistMediaFile) {
                _state.update { YoutubeDownloadConstants.State.Success(videoId) }
            } else {
                youtubeDownloadDataSource.download(youtubeUrl, videoId).onSuccess { dto ->
                    localFileDataSource.saveYoutubeData(
                        videoId,
                        dto.title,
                        dto.audioFilePath,
                        dto.imageFilePath,
                    )
                    _state.update { YoutubeDownloadConstants.State.Success(videoId) }
                }.onFailure { error ->
                    _state.update { YoutubeDownloadConstants.State.Failed(error.message.toString()) }
                }
            }
        }
    }

    private fun extractVideoId(youtubeUrl: String): String? {
        val regex = Regex("v=([a-zA-Z0-9_-]{11})")
        return regex.find(youtubeUrl)?.groupValues?.get(1)
    }
}