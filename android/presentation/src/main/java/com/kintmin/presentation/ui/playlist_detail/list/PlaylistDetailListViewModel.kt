package com.kintmin.presentation.ui.playlist_detail.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_media.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.platform.service_controller.MediaControllerManager
import com.kintmin.platform.service_controller.model.MediaControlData
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
    private val deleteAudioMediaUseCase: DeleteAudioMediaUseCase,
    private val mediaControllerManager: MediaControllerManager,
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
        viewModelScope.launch {
            clickThrottle {
                when (intent) {
                    is PlaylistDetailListIntent.OnClickAudioItem -> playAudioMediaById(intent.data.id)
                    is PlaylistDetailListIntent.OnClickShowDetailAudioMedia -> {
                        triggerEvent(PlaylistDetailListEvent.NavigateToAudioDetailScreen(intent.data.id))
                    }
                    is PlaylistDetailListIntent.OnClickReorderAudioMedia -> {
                        triggerEvent(PlaylistDetailListEvent.NavigateToPlaylistEditScreen(intent.data.id))
                    }
                    is PlaylistDetailListIntent.OnClickEditAudioMedia -> {
                        triggerEvent(PlaylistDetailListEvent.NavigateToAudioEditScreen(intent.data.id))
                    }
                    is PlaylistDetailListIntent.OnClickDeleteAudioMediaInPlaylist -> {
                        deleteAudioMedia(intent.data.id, intent.data.source)
                    }
                    is PlaylistDetailListIntent.OnChangeSearchText -> Unit
                }
            }
        }
    }

    private fun changeSearchText(newSearchText: String) {
        viewModelScope.launch {
            changeSearchTextDebounce {
                _searchText.update { newSearchText }
            }
        }
    }

    private fun deleteAudioMedia(audioMediaId: Int, source: String) {
        viewModelScope.launch {
            deleteAudioMediaUseCase(audioMediaId, source)
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

