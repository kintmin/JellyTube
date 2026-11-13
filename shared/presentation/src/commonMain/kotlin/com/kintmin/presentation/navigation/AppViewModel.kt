package com.kintmin.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackRepeatingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackShufflingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackPitchSemitoneFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackSpeedFlowUseCase
import com.kintmin.domain.playlist.usecase.EnsureSystemPlaylistsUseCase
import com.kintmin.presentation.deeplink.DeepLinkParser
import com.kintmin.presentation.port.MediaControllerManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * 최상위 화면 이동 명령과 앱 시작 시 한 번 하는 일.
 *
 * **플랫폼 진입점이 한 벌만 잡아야 한다.** Compose 밖에서 들어오는 딥링크를 화면과 같은
 * 인스턴스로 넘겨야 한다. 플랫폼은 문자열만 넘기고 판정은 전부 여기서 돈다.
 */
class AppViewModel(
    private val mediaControllerManager: MediaControllerManager,
    private val importSharedAudioMediaUseCase: ImportSharedAudioMediaUseCase,
    private val ensureSystemPlaylistsUseCase: EnsureSystemPlaylistsUseCase,
    private val fetchIsPlaybackRepeatingFlowUseCase: FetchIsPlaybackRepeatingFlowUseCase,
    private val fetchIsPlaybackShufflingFlowUseCase: FetchIsPlaybackShufflingFlowUseCase,
    private val fetchPlaybackSpeedFlowUseCase: FetchPlaybackSpeedFlowUseCase,
    private val fetchPlaybackPitchSemitoneFlowUseCase: FetchPlaybackPitchSemitoneFlowUseCase,
) : ViewModel() {

    private val navigationIntentChannel = Channel<NavigationIntent>(capacity = 8)
    val navigationIntentFlow = navigationIntentChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            ensureSystemPlaylistsUseCase()
        }
        applyPlaybackSettings()
    }

    // 저장한 재생 설정은 화면이 아니라 여기서 플레이어에 건다. 화면에 두면 그 화면을 열기 전까지 적용되지 않는다.
    private fun applyPlaybackSettings() {
        fetchIsPlaybackRepeatingFlowUseCase().onEach(mediaControllerManager::setRepeatMode).launchIn(viewModelScope)
        fetchIsPlaybackShufflingFlowUseCase().onEach(mediaControllerManager::setShuffleMode).launchIn(viewModelScope)
        fetchPlaybackSpeedFlowUseCase().onEach(mediaControllerManager::setPlaybackSpeed).launchIn(viewModelScope)
        fetchPlaybackPitchSemitoneFlowUseCase().onEach(mediaControllerManager::setPlaybackPitchSemitone).launchIn(viewModelScope)
    }

    fun initializeMediaController() {
        mediaControllerManager.initialize()
    }

    /** 앱 밖에서 들어온 주소를 받는 유일한 입구. 우리 주소가 아니면 조용히 아무 일도 하지 않는다. */
    fun handleDeepLink(url: String) {
        viewModelScope.launch {
            DeepLinkParser.parse(
                url = url,
                playingPlaylistId = mediaControllerManager.currentPlaylistId,
                playingMediaId = mediaControllerManager.playingMediaItem?.mediaId?.toIntOrNull(),
            ).forEach { navigationIntent ->
                navigationIntentChannel.send(navigationIntent)
            }
        }
    }

    fun importSharedAudio(fileIdentifiers: List<String>) {
        if (fileIdentifiers.isEmpty()) return

        viewModelScope.launch {
            fileIdentifiers.forEach { fileIdentifier ->
                importSharedAudioMediaUseCase(fileIdentifier)
            }
            navigationIntentChannel.send(NavigationIntent.NavigateToMainPlaylistsTab)
        }
    }
}
