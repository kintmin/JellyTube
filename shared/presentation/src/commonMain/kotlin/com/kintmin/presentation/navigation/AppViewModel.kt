package com.kintmin.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.domain.playlist.usecase.EnsureSystemPlaylistsUseCase
import com.kintmin.domain.user.usecase.RegisterUserUseCase
import com.kintmin.presentation.deeplink.DeepLinkParser
import com.kintmin.presentation.port.MediaControllerManager
import kotlinx.coroutines.channels.Channel
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
    private val registerUserUseCase: RegisterUserUseCase,
    private val importSharedAudioMediaUseCase: ImportSharedAudioMediaUseCase,
    private val ensureSystemPlaylistsUseCase: EnsureSystemPlaylistsUseCase,
) : ViewModel() {

    private val navigationIntentChannel = Channel<NavigationIntent>(capacity = 8)
    val navigationIntentFlow = navigationIntentChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            ensureSystemPlaylistsUseCase()
        }
    }

    fun registerUser() {
        viewModelScope.launch {
            registerUserUseCase()
        }
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
