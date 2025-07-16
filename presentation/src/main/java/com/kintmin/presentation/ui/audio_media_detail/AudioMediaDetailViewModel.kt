package com.kintmin.presentation.ui.audio_media_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_media.usecase.DeleteAudioMediaListUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.presentation.ui.audio_media_detail.navigation.AudioMediaDetailScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioMediaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    private val deleteAudioMediaListUseCase: DeleteAudioMediaListUseCase,
) : ViewModel() {

    private val audioMediaId = savedStateHandle.toRoute<AudioMediaDetailScreenRoute>().audioMediaId

    val data: StateFlow<AudioMediaDetailUiState> = fetchAudioMediaDetailFlowUseCase(audioMediaId)
        .map { it.toAudioMediaDetailUiState() }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), AudioMediaDetailUiState(
                audioMediaId = audioMediaId,
                imageFileFullPath = null,
                audioMediaName = "",
                artist = "",
                playTime = "",
                audioMediaCreationTime = "",
                source = "",
                audioMediaDescription = "",
                playlists = listOf(),
            )
        )

    fun sendIntent(intent: AudioMediaDetailIntent) {
        when (intent) {
            AudioMediaDetailIntent.OnClickDeleteAudioMedia -> deleteAudioMedia()
        }
    }

    private fun deleteAudioMedia() {
        viewModelScope.launch {
            deleteAudioMediaListUseCase(data.play, listOf(audioMediaId))
            _checkedItemIdList.update { emptyList() }
        }
    }
}