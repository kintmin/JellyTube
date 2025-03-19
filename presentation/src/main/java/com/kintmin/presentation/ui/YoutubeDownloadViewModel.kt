package com.kintmin.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.usecase.FetchYoutubeMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YoutubeDownloadViewModel @Inject constructor(
    private val fetchYoutubeMedia: FetchYoutubeMediaUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<YoutubeDownloadConstants.State>(YoutubeDownloadConstants.State.Idle)
    val state = _state.asStateFlow()

    fun startDownload(youtubeUrl: String) {
        viewModelScope.launch {
            _state.update { YoutubeDownloadConstants.State.Loading }

            fetchYoutubeMedia(youtubeUrl).onSuccess { media ->
                _state.update { YoutubeDownloadConstants.State.Success(media) }
            }.onFailure { throwable ->
                _state.update { YoutubeDownloadConstants.State.Failed(throwable.message.toString()) }
            }
        }
    }
}