package com.kintmin.presentation.ui.secret_playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.app_setting.usecase.UpdatePlaylistIdOnDownloadUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.AddNewPlaylistUseCase
import com.kintmin.domain.playlist.usecase.DeletePlaylistUseCase
import com.kintmin.domain.playlist.usecase.FetchDownloadTargetPlaylistsFlowUseCase
import com.kintmin.domain.playlist.usecase.FetchPlaylistsByTypeUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistSequenceUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeMinutesFlowUseCase
import com.kintmin.domain.secret_mode.usecase.UpdateSecretModeMinutesUseCase
import com.kintmin.presentation.secret_mode.SecretModeRunner
import com.kintmin.presentation.ui.main.playlist.PlaylistEvent
import com.kintmin.presentation.ui.main.playlist.PlaylistIntent
import com.kintmin.presentation.ui.main.playlist.toUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SecretPlaylistViewModel(
    private val secretModeRunner: SecretModeRunner,
    fetchSecretModeEnabledFlowUseCase: FetchSecretModeEnabledFlowUseCase,
    fetchSecretModeMinutesFlowUseCase: FetchSecretModeMinutesFlowUseCase,
    fetchPlaylistsByTypeUseCase: FetchPlaylistsByTypeUseCase,
    fetchDownloadTargetPlaylistsFlowUseCase: FetchDownloadTargetPlaylistsFlowUseCase,
    private val updateSecretModeMinutesUseCase: UpdateSecretModeMinutesUseCase,
    private val updatePlaylistIdOnDownloadUseCase: UpdatePlaylistIdOnDownloadUseCase,
    private val addNewPlaylistUseCase: AddNewPlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val updatePlaylistSequenceUseCase: UpdatePlaylistSequenceUseCase,
) : ViewModel() {

    private val eventChannel = Channel<SecretPlaylistEvent>(Channel.BUFFERED)
    val eventFlow = eventChannel.receiveAsFlow()

    private val isReorderBottomSheetVisible = MutableStateFlow(false)

    private val isEnabledFlow = fetchSecretModeEnabledFlowUseCase()

    private val remainingFlow = isEnabledFlow.flatMapLatest { isEnabled ->
        if (!isEnabled) return@flatMapLatest flowOf<Duration?>(null)
        flow {
            while (true) {
                emit(secretModeRunner.remaining)
                delay(1.seconds)
            }
        }
    }

    // 남은 시간만 매초 바뀐다. 목록 매핑과 따로 묶어 매초 다시 하지 않는다.
    private val baseUiStateFlow = combine(
        isEnabledFlow,
        fetchSecretModeMinutesFlowUseCase(),
        fetchPlaylistsByTypeUseCase(PlaylistType.SECRET),
        fetchDownloadTargetPlaylistsFlowUseCase(),
        isReorderBottomSheetVisible,
    ) { isEnabled, minutes, playlists, downloadTarget, isReorderVisible ->
        SecretPlaylistUiState(
            isEnabled = isEnabled,
            minutes = minutes,
            playlists = playlists.map { it.toUiModel(isDownloadTarget = it.id == downloadTarget.targetId) },
            isReorderBottomSheetVisible = isReorderVisible,
        )
    }

    val uiState: StateFlow<SecretPlaylistUiState> = combine(baseUiStateFlow, remainingFlow) { base, remaining ->
        base.copy(remaining = remaining)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecretPlaylistUiState())

    fun sendIntent(intent: SecretPlaylistIntent) {
        when (intent) {
            is SecretPlaylistIntent.OnToggleSecretMode -> toggleSecretMode(intent.isEnabled)
            is SecretPlaylistIntent.OnSelectMinutes -> viewModelScope.launch { updateSecretModeMinutesUseCase(intent.minutes) }
            is SecretPlaylistIntent.OnPlaylistIntent -> handlePlaylistIntent(intent.intent)
        }
    }

    private fun toggleSecretMode(isEnabled: Boolean) {
        if (isEnabled) secretModeRunner.start(uiState.value.minutes.minutes) else secretModeRunner.end()
    }

    private fun handlePlaylistIntent(intent: PlaylistIntent) {
        viewModelScope.launch {
            when (intent) {
                is PlaylistIntent.OnClickPlaylistItem ->
                    eventChannel.send(SecretPlaylistEvent.NavigateToPlaylistDetailScreen(intent.playlistInfo.id))
                is PlaylistIntent.OnClickModifyPlaylist ->
                    eventChannel.send(SecretPlaylistEvent.NavigateToPlaylistEditScreen(intent.data.id))
                is PlaylistIntent.OnClickAddPlaylist ->
                    eventChannel.send(SecretPlaylistEvent.NavigateToPlaylistAddScreen(intent.data.id, intent.data.name))
                is PlaylistIntent.MakeNewPlaylist -> addNewPlaylistUseCase(intent.title, PlaylistType.SECRET).onFailure {
                    eventChannel.send(SecretPlaylistEvent.ShowToast(PlaylistEvent.MAKE_FAILED_MESSAGE))
                }
                is PlaylistIntent.OnClickDeletePlaylist -> deletePlaylistUseCase(intent.data.id).onFailure {
                    eventChannel.send(SecretPlaylistEvent.ShowToast(PlaylistEvent.DELETE_FAILED_MESSAGE))
                }
                is PlaylistIntent.OnClickSetDownloadTarget -> {
                    val message = updatePlaylistIdOnDownloadUseCase(intent.data.id).fold(
                        onSuccess = { "비밀모드가 끝날 때까지 다운로드한 곡은 '${intent.data.name}'에 담깁니다." },
                        onFailure = { PlaylistEvent.SET_DOWNLOAD_TARGET_FAILED_MESSAGE },
                    )
                    eventChannel.send(SecretPlaylistEvent.ShowToast(message))
                }
                PlaylistIntent.OnClickShowReorderBottomSheet -> isReorderBottomSheetVisible.value = true
                PlaylistIntent.OnDismissReorderBottomSheet -> isReorderBottomSheetVisible.value = false
                is PlaylistIntent.OnReorderPlaylist -> updatePlaylistSequenceUseCase(intent.orderedIds).onFailure {
                    // 쓰기가 실패하면 DB 가 그대로라 Flow 가 재방출되지 않는다. 말해 주지 않으면
                    // 시트는 새 순서를, 격자는 옛 순서를 동시에 보여준다.
                    eventChannel.send(SecretPlaylistEvent.ShowToast(PlaylistEvent.REORDER_FAILED_MESSAGE))
                }
            }
        }
    }
}
