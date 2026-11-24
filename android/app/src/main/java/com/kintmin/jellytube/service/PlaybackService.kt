package com.kintmin.jellytube.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.kintmin.domain.secret_mode.usecase.FetchSecretAudioMediaIdListFlowUseCase
import com.kintmin.jellytube.intent.mediaSessionPendingIntent
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.secret
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlaybackService : MediaSessionService(), KoinComponent {

    private val appLog: AppLog by inject()
    private val fetchSecretAudioMediaIdListFlowUseCase: FetchSecretAudioMediaIdListFlowUseCase by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var mediaSession: MediaSession? = null

    // 리스너는 동기라 그때 조회할 수 없다. 첫 값이 오기 전(null)에는 판정할 수 없어 가린다.
    private val secretAudioMediaIds =
        fetchSecretAudioMediaIdListFlowUseCase().map { it.toSet() }.stateIn(serviceScope, SharingStarted.Eagerly, null)

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_GAME) // ducking 방지
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        appLog.sendErrorLog(
                            ErrorLog(LOG_TAG, "${error.errorCodeName} mediaId=${currentMediaItem?.mediaId}", error),
                        )
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val why = when (reason) {
                            Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "반복"
                            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "다음 곡 자동"
                            Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "곡 이동"
                            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "재생목록 바뀜"
                            else -> "reason=$reason"
                        }
                        log("곡 바뀜($why): mediaId=${mediaItem?.mediaId} ${secret(mediaItem?.mediaMetadata?.title, isSecret(mediaItem))}")
                    }

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        val why = when (reason) {
                            Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> "사용자·리모컨"
                            Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> "오디오 포커스 잃음"
                            Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> "이어폰 빠짐"
                            Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> "원격"
                            Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> "곡 끝"
                            else -> "reason=$reason"
                        }
                        log("${if (playWhenReady) "재생" else "일시정지"}($why): mediaId=${currentMediaItem?.mediaId}")
                    }
                })
            }

        // https://developer.android.com/reference/androidx/media3/session/MediaSession.Callback
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(mediaSessionPendingIntent())
            .build()
    }

    private fun isSecret(mediaItem: MediaItem?): Boolean {
        val secretIds = secretAudioMediaIds.value ?: return true
        return mediaItem?.mediaId?.toIntOrNull() in secretIds
    }

    private fun log(message: String) = appLog.sendDebugLog(DebugLog(LOG_TAG, message))

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

private const val LOG_TAG = "PlaybackService"
