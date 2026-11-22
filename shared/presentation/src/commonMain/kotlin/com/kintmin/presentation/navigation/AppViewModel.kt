package com.kintmin.presentation.navigation

import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackRepeatingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackShufflingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackPitchSemitoneFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackSpeedFlowUseCase
import com.kintmin.domain.backup.usecase.BackupIfDueUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.EnsureSystemPlaylistsUseCase
import com.kintmin.domain.playlist.usecase.FetchPlaylistsByTypeUseCase
import com.kintmin.domain.secret_mode.usecase.EndSecretModeUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretAudioMediaIdListFlowUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import com.kintmin.presentation.deeplink.DeepLinkParser
import com.kintmin.presentation.platform.continueInBackgroundWhile
import com.kintmin.presentation.port.PlaybackController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Compose 밖에서 들어오는 딥링크를 화면과 같은 인스턴스로 넘겨야 해서 한 벌만 잡는다.
 */
class AppViewModel(
    private val mediaControllerManager: PlaybackController,
    private val importSharedAudioMediaUseCase: ImportSharedAudioMediaUseCase,
    private val ensureSystemPlaylistsUseCase: EnsureSystemPlaylistsUseCase,
    private val fetchIsPlaybackRepeatingFlowUseCase: FetchIsPlaybackRepeatingFlowUseCase,
    private val fetchIsPlaybackShufflingFlowUseCase: FetchIsPlaybackShufflingFlowUseCase,
    private val fetchPlaybackSpeedFlowUseCase: FetchPlaybackSpeedFlowUseCase,
    private val fetchPlaybackPitchSemitoneFlowUseCase: FetchPlaybackPitchSemitoneFlowUseCase,
    private val fetchSecretModeEnabledFlowUseCase: FetchSecretModeEnabledFlowUseCase,
    private val endSecretModeUseCase: EndSecretModeUseCase,
    private val fetchPlaylistsByTypeUseCase: FetchPlaylistsByTypeUseCase,
    private val fetchSecretAudioMediaIdListFlowUseCase: FetchSecretAudioMediaIdListFlowUseCase,
    private val backupIfDueUseCase: BackupIfDueUseCase,
    private val appLog: AppLog,
) : ViewModel() {

    private val eventChannel = Channel<NavigationEvent>(capacity = 8)
    val eventFlow = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            ensureSystemPlaylistsUseCase()
                .onFailure { appLog.sendErrorLog(ErrorLog("AppViewModel", "시스템 플레이리스트 보장 실패", it)) }
            // 비밀모드 중에 앱이 죽으면 다운로드 대상이 비밀 플레이리스트로 남는다. 켜져 있으면
            // 재생이 프로세스를 살려 둔 채 화면만 다시 뜬 것이라 끄지 않는다.
            if (!fetchSecretModeEnabledFlowUseCase().value) endSecretModeUseCase()
        }
        applyPlaybackSettings()
        leaveSecretScreensWhenSecretModeEnds()
    }

    private fun leaveSecretScreensWhenSecretModeEnds() {
        // 첫 값도 받는다. 프로세스가 죽은 뒤 복원된 화면 스택에 비밀 화면이 남아 있을 수 있다.
        fetchSecretModeEnabledFlowUseCase().filter { !it }.onEach {
            eventChannel.send(NavigationEvent.LeaveSecretScreens)
        }.launchIn(viewModelScope)
    }

    private suspend fun secretPlaylistIds(): Set<Int> =
        fetchPlaylistsByTypeUseCase(PlaylistType.SECRET).first().mapTo(HashSet()) { it.id }

    private fun applyPlaybackSettings() {
        fetchIsPlaybackRepeatingFlowUseCase().onEach(mediaControllerManager::setRepeatMode).launchIn(viewModelScope)
        fetchIsPlaybackShufflingFlowUseCase().onEach(mediaControllerManager::setShuffleMode).launchIn(viewModelScope)
        fetchPlaybackSpeedFlowUseCase().onEach(mediaControllerManager::setPlaybackSpeed).launchIn(viewModelScope)
        fetchPlaybackPitchSemitoneFlowUseCase().onEach(mediaControllerManager::setPlaybackPitchSemitone).launchIn(viewModelScope)
    }

    fun sendIntent(intent: AppIntent) {
        when (intent) {
            AppIntent.InitializeMediaController -> initializeMediaController()
            is AppIntent.HandleDeepLink -> handleDeepLink(intent.url)
            is AppIntent.ImportSharedAudio -> importSharedAudio(intent.fileIdentifiers)
            AppIntent.OnAppBackground -> backupIfDue()
        }
    }

    private fun backupIfDue() {
        viewModelScope.launch {
            continueInBackgroundWhile { backupIfDueUseCase(Clock.System.now().toEpochMilliseconds()) }
        }
    }

    private fun initializeMediaController() {
        mediaControllerManager.initialize()
    }

    /** 앱 밖에서 들어온 주소를 받는 유일한 입구. 우리 주소가 아니면 조용히 아무 일도 하지 않는다. */
    private fun handleDeepLink(url: String) {
        viewModelScope.launch {
            DeepLinkParser.parse(
                url = url,
                playingPlaylistId = mediaControllerManager.currentPlaylistId,
                playingMediaId = mediaControllerManager.playingMediaItem?.mediaId?.toIntOrNull(),
            ).let { events ->
                if (events.isEmpty()) log("우리 주소가 아니거나 형식이 틀림: $url")
                // 비밀 플레이리스트 뒤에 쌓이는 곡 상세도 그 플레이리스트의 것이다. 거기서부터 버린다.
                val hiddenIndex = events.indexOfFirst { it.isHiddenSecret() }
                if (hiddenIndex >= 0) log("비밀 화면이라 ${hiddenIndex}번째 이동부터 버림: $url")
                if (hiddenIndex < 0) events else events.take(hiddenIndex)
            }.also { events ->
                if (events.isNotEmpty()) log("받음: $url → ${events.joinToString()}")
            }.forEach { navigationEvent ->
                eventChannel.send(navigationEvent)
            }
        }
    }

    private suspend fun NavigationEvent.isHiddenSecret(): Boolean {
        if (fetchSecretModeEnabledFlowUseCase().value) return false
        return when (this) {
            is NavigationEvent.NavigateToPlaylistContent -> playlistId in secretPlaylistIds()
            is NavigationEvent.NavigateToAudioMedia -> audioMediaId in secretAudioMediaIds()
            is NavigationEvent.NavigateToLyricsViewer -> audioMediaId in secretAudioMediaIds()
            else -> false
        }
    }

    private fun log(message: String) = appLog.sendDebugLog(DebugLog("DeepLink", message))

    private suspend fun secretAudioMediaIds(): List<Int> = fetchSecretAudioMediaIdListFlowUseCase().first()

    private fun importSharedAudio(fileIdentifiers: List<String>) {
        if (fileIdentifiers.isEmpty()) return

        viewModelScope.launch {
            fileIdentifiers.forEach { fileIdentifier ->
                importSharedAudioMediaUseCase(fileIdentifier)
            }
            eventChannel.send(NavigationEvent.NavigateToMainPlaylistsTab)
        }
    }
}
