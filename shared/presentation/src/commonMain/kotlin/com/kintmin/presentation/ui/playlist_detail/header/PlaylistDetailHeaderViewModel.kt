package com.kintmin.presentation.ui.playlist_detail.header

import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackRepeatingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackShufflingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdateIsPlaybackShufflingUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackRepeatingUseCase
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.domain.playlist.usecase.FetchPlaylistFlowUseCase
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.port.model.toMediaControlData
import com.kintmin.presentation.ui.playlist_detail.navigation.PlaylistDetailScreenRoute
import com.kintmin.presentation.util.Throttle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration

class PlaylistDetailHeaderViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaControllerManager: PlaybackController,
    fetchPlaylistFlowUseCase: FetchPlaylistFlowUseCase,
    fetchIsPlaybackRepeatingFlowUseCase: FetchIsPlaybackRepeatingFlowUseCase,
    fetchIsPlaybackShufflingFlowUseCase: FetchIsPlaybackShufflingFlowUseCase,
    private val updatePlaybackRepeatingUseCase: UpdatePlaybackRepeatingUseCase,
    private val updateIsPlaybackShufflingUseCase: UpdateIsPlaybackShufflingUseCase,
    private val fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
    private val appLog: AppLog,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PlaylistDetailScreenRoute>()
    private val playlistId = route.playlistId

    private val eventChannel = Channel<PlaylistDetailHeaderEvent>(Channel.BUFFERED)
    val eventFlow: Flow<PlaylistDetailHeaderEvent> = eventChannel.receiveAsFlow()

    private val clickThrottle = Throttle(300L)

    // 비밀 '찜' 에서는 저장된 집계가 즐겨찾기 전체의 것이라 목록과 맞지 않다. 그때만 걸러 낸 목록으로 센다.
    private val secretOnlyTracksFlow: Flow<List<PlaylistTrackAggregate>?> =
        if (route.secretOnly) fetchAudioMediaListFlowUseCase(playlistId, route.secretTrackFilter) else flowOf(null)

    val uiState: StateFlow<PlaylistDetailHeaderUiState> =
        combine(
            fetchPlaylistFlowUseCase(playlistId),
            fetchIsPlaybackRepeatingFlowUseCase(),
            fetchIsPlaybackShufflingFlowUseCase(),
            secretOnlyTracksFlow,
        ) { playlist, isRepeating, isShuffling, secretOnlyTracks ->
            val audioMediaCount = secretOnlyTracks?.size ?: playlist.audioMediaCount
            val playTimeDuration = secretOnlyTracks
                ?.fold(Duration.ZERO) { sum, track -> sum + (track.audioMedia.playbackDuration ?: Duration.ZERO) }
                ?: playlist.playTimeDuration
            PlaylistDetailHeaderUiState(
                id = playlist.id,
                imageFileFullPath = playlist.imageFileFullPath,
                name = playlist.name,
                description = playlist.description,
                // 어느 범위를 보고 있는지 머리말이 말한다. 같은 즐겨찾기라도 담긴 곡이 다르다.
                playlistSubtitle = "${if (route.secretOnly) "비밀 플레이리스트의 곡" else "플레이리스트"}" +
                    " · 음원수 $audioMediaCount · 재생시간 ${playTimeDuration.to_hh_colon_mm_colon_ss()}",
                isRepeating = isRepeating,
                isShuffling = isShuffling,
                isBasePlaylist = playlist.isBasePlaylist,
                canRemoveTrack = !playlist.type.isAutoManaged,
            )
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistDetailHeaderUiState(
                id = 0,
                imageFileFullPath = null,
                name = "",
                description = "",
                playlistSubtitle = "",
                isRepeating = false,
                isShuffling = false,
                isBasePlaylist = route.isBasePlaylist,
            )
        )


    fun sendIntent(intent: PlaylistDetailHeaderIntent) {
        viewModelScope.launch {
            clickThrottle {
                when (intent) {
                    PlaylistDetailHeaderIntent.OnClickAdd -> triggerEvent(PlaylistDetailHeaderEvent.NavigateToAddAudioMediaScreen)
                    PlaylistDetailHeaderIntent.OnClickEdit -> triggerEvent(PlaylistDetailHeaderEvent.NavigateToEditPlaylistScreen)
                    PlaylistDetailHeaderIntent.OnClickPlay -> playbackPlaylist()
                    PlaylistDetailHeaderIntent.OnClickRepeat -> updatePlaybackRepeating()
                    PlaylistDetailHeaderIntent.OnClickShuffle -> updateIsPlaybackShuffling()
                }
            }
        }
    }

    private fun playbackPlaylist() {
        viewModelScope.launch {
            val mediaControlDataList = fetchAudioMediaListFlowUseCase(playlistId, route.secretTrackFilter)
                .first().map { it.audioMedia.toMediaControlData() }
            mediaControllerManager.playFromPlaylist(playlistId, null, mediaControlDataList)
                .onFailure { appLog.sendErrorLog(ErrorLog("PlaylistDetailHeader", "재생 시작 실패: playlistId=$playlistId", it)) }
        }
    }

    private fun updatePlaybackRepeating() {
        viewModelScope.launch {
            val newValue = !uiState.value.isRepeating
            updatePlaybackRepeatingUseCase(newValue)
        }
    }

    private fun updateIsPlaybackShuffling() {
        viewModelScope.launch {
            val newValue = !uiState.value.isShuffling
            updateIsPlaybackShufflingUseCase(newValue)
        }
    }

    private fun triggerEvent(newEvent: PlaylistDetailHeaderEvent) {
        viewModelScope.launch {
            eventChannel.send(newEvent)
        }
    }
}
