package com.kintmin.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackRepeatingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackShufflingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackPitchSemitoneFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackSpeedFlowUseCase
import com.kintmin.domain.playlist.usecase.EnsureSystemPlaylistsUseCase
import com.kintmin.domain.secret_mode.usecase.EndSecretModeUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import com.kintmin.presentation.deeplink.DeepLinkParser
import com.kintmin.presentation.port.PlaybackController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

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
) : ViewModel() {

    private val eventChannel = Channel<NavigationEvent>(capacity = 8)
    val eventFlow = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            ensureSystemPlaylistsUseCase()
            // 비밀모드 중에 앱이 죽으면 다운로드 대상이 비밀 플레이리스트로 남는다. 켜져 있으면
            // 재생이 프로세스를 살려 둔 채 화면만 다시 뜬 것이라 끄지 않는다.
            if (!fetchSecretModeEnabledFlowUseCase().value) endSecretModeUseCase()
        }
        applyPlaybackSettings()
    }

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
            ).forEach { navigationEvent ->
                eventChannel.send(navigationEvent)
            }
        }
    }

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
