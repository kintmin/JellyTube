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
 * 화면 하나에 속하지 않는 앱 레벨 상태 — 최상위 화면 이동 명령과 앱 시작 시 한 번 하는 일.
 *
 * **플랫폼 진입점이 한 벌만 잡아야 한다.** Compose 밖에서 들어오는 딥링크(Android `Intent`,
 * iOS `onOpenURL`)를 화면과 같은 인스턴스로 넘겨야 하기 때문이다.
 *
 * 플랫폼에서 오는 것은 전부 문자열이다. `Intent` 해석은 `android:app` 이, URL 수신은 iOS
 * 진입점이 하고, 그 뒤 판정은 전부 여기서 공통으로 돈다.
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

    /**
     * 앱 밖에서 들어온 주소를 받는 유일한 입구.
     *
     * 우리 주소가 아니면 [DeepLinkParser] 가 빈 목록을 돌려주고 아무 일도 나지 않는다.
     * 실패를 사용자에게 알리지 않는다 — 앱 밖에서 온 주소는 사용자가 의도한 것이 아닐 수 있다.
     */
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

    /** 외부 앱이 공유한 오디오를 가져온다. 플랫폼은 식별자 문자열만 넘긴다. */
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
