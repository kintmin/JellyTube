package com.kintmin.presentation.ui.playlist_detail.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.platform.util.MediaControllerManager
import com.kintmin.presentation.ui.playlist_detail.navigation.PlaylistDetailScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaControllerManager: MediaControllerManager,
    fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
    private val deleteAudioMediaUseCase: DeleteAudioMediaUseCase,
) : ViewModel() {

    private val playlistId = savedStateHandle.toRoute<PlaylistDetailScreenRoute>().playlistId

    private val _eventFlow = MutableSharedFlow<PlaylistDetailListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val audioListFlow = fetchAudioMediaListFlowUseCase(playlistId).map { list -> list.map { it.toUiModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendIntent(intent: PlaylistDetailListIntent) {
        when (intent) {
            is PlaylistDetailListIntent.OnClickAudioItem -> playAudioMediaById(intent.data.id)
            is PlaylistDetailListIntent.OnClickDeleteAudioMediaFile -> deleteAudioMediaFile(intent.data.id)
            is PlaylistDetailListIntent.OnClickShowDetailAudioMedia -> {
                triggerEvent(PlaylistDetailListEvent.NavigateToAudioDetailScreen)
                // 타이틀 수정
                // 설명 수정
                // 아티스트 수정
                // 이미지 수정
                // 플레이리스트 수정 (다수 선택 가능)
                // 음원 제거
            }
            is PlaylistDetailListIntent.OnClickDeleteAudioMediaInPlaylist -> deleteAudioMediaInPlaylist(intent.data.id)
        }
    }

    private fun playAudioMediaById(id: Int) {
        mediaControllerManager.playFromPlaylist(playlistId, id)
    }

    private fun deleteAudioMediaFile(id: Int) {
        viewModelScope.launch {
            mediaControllerManager.tryDeleteMediaItem(playlistId, id)
            deleteAudioMediaUseCase(id)
        }
    }

    private fun deleteAudioMediaInPlaylist(id: Int) {}

    private fun triggerEvent(newEvent: PlaylistDetailListEvent) {
        viewModelScope.launch {
            _eventFlow.emit(newEvent)
        }
    }
}