package com.kintmin.jellytube

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.domain.playlist.usecase.EnsureSystemPlaylistsUseCase
import com.kintmin.domain.user.usecase.RegisterUserUseCase
import com.kintmin.presentation.deeplink.DeepLinkParser
import com.kintmin.presentation.navigation.NavigationIntent
import com.kintmin.presentation.port.MediaControllerManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainViewModel(
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

    fun handleIntent(intent: Intent?) {
        intent ?: return

        // 딥링크 처리
        val uri = intent.data
        if (uri != null) {
            intent.data = null  // 사용된 딥링크는 소비
            onDeepLink(uri.toString())
            return
        }

        // Quick Share / 공유 시트를 통한 오디오 파일 수신
        when (intent.action) {
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val sharedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (sharedUri != null) {
                    intent.removeExtra(Intent.EXTRA_STREAM)  // 재처리 방지
                    handleSharedAudioUris(listOf(sharedUri))
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                val sharedUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!sharedUris.isNullOrEmpty()) {
                    intent.removeExtra(Intent.EXTRA_STREAM)  // 재처리 방지
                    handleSharedAudioUris(sharedUris)
                }
            }
        }
    }

    private fun handleSharedAudioUris(uris: List<Uri>) {
        viewModelScope.launch {
            uris.forEach { uri ->
                importSharedAudioMediaUseCase(uri.toString())
            }
            navigationIntentChannel.send(NavigationIntent.NavigateToMainPlaylistsTab)
        }
    }

    /**
     * 앱 밖에서 들어온 주소를 받는 유일한 입구.
     *
     * 판정과 명령 생성은 [DeepLinkParser] 가 한다 — 안드로이드와 iOS 가 같은 답을 내야 하므로
     * 이 자리에 플랫폼 분기를 두지 않는다. 우리 주소가 아니면 빈 목록이 와서 아무 일도 나지 않는다.
     */
    fun onDeepLink(url: String) {
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
}
