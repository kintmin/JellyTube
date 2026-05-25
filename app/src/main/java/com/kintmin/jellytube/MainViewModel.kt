package com.kintmin.jellytube

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.user.usecase.RegisterUserUseCase
import com.kintmin.platform.deeplink.DeepLinkConstants
import com.kintmin.platform.service_controller.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaControllerManager: MediaControllerManager,
    private val registerUserUseCase: RegisterUserUseCase,
) : ViewModel() {

    private val navigationIntentChannel = Channel<NavigationIntent>(capacity = 8)
    val navigationIntentFlow = navigationIntentChannel.receiveAsFlow()

    fun registerUser() {
        viewModelScope.launch {
            registerUserUseCase()
        }
    }

    fun initializeMediaController() {
        mediaControllerManager.initialize()
    }

    fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        intent.data = null  // 사용된 딥링크는 소비
        onDeepLink(uri)
    }

    fun onDeepLink(uri: Uri) {
        viewModelScope.launch {
            sendNavigationIntents(uri)
        }
    }

    private suspend fun sendNavigationIntents(uri: Uri) {
        if (uri.scheme != DeepLinkConstants.DEEP_LINK_SCHEME) return
        if (uri.host != DeepLinkConstants.DEEP_LINK_HOST) return

        val pathList = uri.pathSegments

        val rootPath = pathList.getOrNull(0)
        if (rootPath != DeepLinkConstants.Path.MAIN) return

        navigationIntentChannel.send(NavigationIntent.PopAll)

        val mainPath = pathList.getOrNull(1)
        when (mainPath) {
            DeepLinkConstants.Path.SETTINGS -> {
                val settingsPath = pathList.getOrNull(2)
                navigationIntentChannel.send(NavigationIntent.NavigateToMainPlaylistsTab)
                navigationIntentChannel.send(NavigationIntent.NavigateToSettings)

                when (settingsPath) {
                    DeepLinkConstants.Path.APP_LOG -> {
                        navigationIntentChannel.send(NavigationIntent.NavigateToSettingAppLog)
                    }
                }
            }
            DeepLinkConstants.Path.STEP -> {
                navigationIntentChannel.send(NavigationIntent.NavigateToMainPlaylistsTab)
                navigationIntentChannel.send(NavigationIntent.NavigateToSettings)
                navigationIntentChannel.send(NavigationIntent.NavigateToStep)
            }
            DeepLinkConstants.Path.DOWNLOAD -> {
                val encodedUrl = uri.getQueryParameter(DeepLinkConstants.QueryKey.ENCODED_URL)
                val targetUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                navigationIntentChannel.send(NavigationIntent.NavigateToMainDownloadTab(targetUrl))
            }
            DeepLinkConstants.Path.PLAYER -> {
                navigationIntentChannel.send(NavigationIntent.NavigateToMainPlaylistsTab)

                val entry = uri.getQueryParameter(DeepLinkConstants.QueryKey.ENTRY)
                when (entry) {
                    DeepLinkConstants.Path.PLAYLISTS -> {
                        val currentPlaylistId = mediaControllerManager.currentPlaylistId
                        if (currentPlaylistId == null) {
                            navigationIntentChannel.send(NavigationIntent.NavigateToPlayer)
                        } else {
                            val mediaId = mediaControllerManager.playingMediaItem?.mediaId?.toIntOrNull()
                            navigationIntentChannel.send(NavigationIntent.NavigateToPlaylistContent(playlistId = currentPlaylistId, focusAudioMediaId = mediaId))
                            navigationIntentChannel.send(NavigationIntent.NavigateToPlayer)
                        }
                    }
                    else -> {
                        navigationIntentChannel.send(NavigationIntent.NavigateToPlayer)
                    }
                }
            }
            DeepLinkConstants.Path.PLAYLISTS -> {
                navigationIntentChannel.send(NavigationIntent.NavigateToMainPlaylistsTab)

                val playlistId = pathList.getOrNull(2)?.toIntOrNull() ?: return
                val focusAudioMediaId = uri.getQueryParameter(DeepLinkConstants.QueryKey.FOCUS_AUDIO_MEDIA_ID)?.toIntOrNull()

                navigationIntentChannel.send(NavigationIntent.NavigateToPlaylistContent(playlistId = playlistId, focusAudioMediaId = focusAudioMediaId))

                val audioMediaId = if (pathList.getOrNull(3) == DeepLinkConstants.Path.AUDIO_MEDIAS) {
                    pathList.getOrNull(4)?.toIntOrNull()
                } else {
                    null
                }

                if (audioMediaId != null) {
                    navigationIntentChannel.send(NavigationIntent.NavigateToAudioMedia(audioMediaId))
                }
            }
        }
    }
}
