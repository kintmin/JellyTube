package com.kintmin.presentation.ui.playlist_detail.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.usecase.DeleteAudioTrackListUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.domain.audio_track.usecase.UpdateTrackSequenceUseCase
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.port.model.MediaControlData
import com.kintmin.presentation.extension.matchKorean
import com.kintmin.presentation.ui.playlist_detail.navigation.PlaylistDetailScreenRoute
import com.kintmin.presentation.util.Debounce
import com.kintmin.presentation.util.Throttle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaylistDetailListViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
    private val deleteAudioTrackListUseCase: DeleteAudioTrackListUseCase,
    private val updateTrackSequenceUseCase: UpdateTrackSequenceUseCase,
    private val mediaControllerManager: PlaybackController,
) : ViewModel() {

    private val playlistId = savedStateHandle.toRoute<PlaylistDetailScreenRoute>().playlistId

    private val _eventFlow = MutableSharedFlow<PlaylistDetailListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val clickThrottle = Throttle(300L)

    private val changeSearchTextDebounce = Debounce(200L)
    private val _searchText = MutableStateFlow("")

    // 재생 큐 소스 — 필터링과 무관하게 항상 플레이리스트 전체 데이터
    private val fullAudioListFlow =
        fetchAudioMediaListFlowUseCase(playlistId).map { list -> list.map { it.toPlaylistDetailListItemUiState() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 화면 표시용 — 검색어로 실시간 필터링
    val audioListFlow =
        combine(fullAudioListFlow, _searchText) { list, searchText ->
            list.filter { it.mediaName.matchKorean(searchText) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendIntent(intent: PlaylistDetailListIntent) {
        if (intent is PlaylistDetailListIntent.OnChangeSearchText) {
            changeSearchText(intent.searchText)
            return
        }
        // 끌어 놓을 때마다 오므로 클릭 throttle 에 걸리면 연속 재정렬이 먹힌다.
        if (intent is PlaylistDetailListIntent.OnReorderAudioMedia) {
            updateTrackSequence(intent.reorderData, intent.targetData)
            return
        }
        viewModelScope.launch {
            clickThrottle {
                when (intent) {
                    is PlaylistDetailListIntent.OnClickAudioItem -> playAudioMediaById(intent.data.id)
                    is PlaylistDetailListIntent.OnClickShowDetailAudioMedia -> {
                        triggerEvent(PlaylistDetailListEvent.NavigateToAudioDetailScreen(intent.data.id))
                    }
                    is PlaylistDetailListIntent.OnClickEditPlaylist -> {
                        triggerEvent(PlaylistDetailListEvent.NavigateToPlaylistEditScreen(intent.data.id))
                    }
                    is PlaylistDetailListIntent.OnClickEditAudioMedia -> {
                        triggerEvent(PlaylistDetailListEvent.NavigateToAudioEditScreen(intent.data.id))
                    }
                    is PlaylistDetailListIntent.OnClickRemoveFromPlaylist -> {
                        removeFromPlaylist(intent.data.id)
                    }
                    is PlaylistDetailListIntent.OnChangeSearchText,
                    is PlaylistDetailListIntent.OnReorderAudioMedia -> Unit
                }
            }
        }
    }

    private fun updateTrackSequence(
        reorderData: PlaylistDetailListItemUiState,
        targetData: PlaylistDetailListItemUiState,
    ) {
        if (reorderData.id == targetData.id) return

        viewModelScope.launch {
            updateTrackSequenceUseCase(playlistId, reorderData.id, reorderData.sequence, targetData.sequence)
                .onFailure {
                    // 쓰기가 실패하면 DB 가 그대로라 Flow 가 재방출되지 않는다. 말해 주지 않으면
                    // 목록은 새 순서를, 재생 큐는 옛 순서를 쓴 채로 남는다.
                    triggerEvent(PlaylistDetailListEvent.ShowToast("순서를 저장하지 못했습니다. 잠시 뒤 다시 시도해 주세요."))
                }
        }
    }

    private fun changeSearchText(newSearchText: String) {
        viewModelScope.launch {
            changeSearchTextDebounce {
                _searchText.update { newSearchText }
            }
            // 지우는 것은 미루지 않는다. 미루면 빈 검색어와 걸러진 목록이 debounce 만큼
            // 어긋나, 곡이 있는 플레이리스트가 그 동안 "담긴 음원이 없습니다" 로 보인다.
            // 위에서 먼저 호출해 밀린 작업을 취소시킨 뒤라 되살아나지 않는다.
            if (newSearchText.isEmpty()) {
                _searchText.update { "" }
            }
        }
    }

    private fun removeFromPlaylist(audioMediaId: Int) {
        viewModelScope.launch {
            deleteAudioTrackListUseCase(playlistId, listOf(audioMediaId))
        }
    }

    private fun playAudioMediaById(id: Int) {
        viewModelScope.launch {
            val mediaControlDataList = fullAudioListFlow.value.map {
                MediaControlData(
                    mediaId = it.id.toString(),
                    mediaFileUri = it.audioFileFullPath,
                    mediaTitle = it.mediaName,
                    mediaDescription = it.description,
                    mediaArtist = it.artist,
                    mediaDurationMs = it.audioDuration?.inWholeMilliseconds ?: 0L,
                    mediaArtworkFileUri = it.imageFileFullPath,
                )
            }
            mediaControllerManager.playFromPlaylist(playlistId, id, mediaControlDataList)
        }
    }

    private fun triggerEvent(newEvent: PlaylistDetailListEvent) {
        viewModelScope.launch {
            _eventFlow.emit(newEvent)
        }
    }
}

