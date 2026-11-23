package com.kintmin.presentation.secret_mode

import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistsByTypeUseCase
import com.kintmin.domain.secret_mode.usecase.EndSecretModeUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretAudioMediaIdListFlowUseCase
import com.kintmin.domain.secret_mode.usecase.StartSecretModeUseCase
import com.kintmin.presentation.port.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * **화면이 아니라 앱에 하나만 있어야 한다.** 화면을 떠나도 정한 시간이 지나면 꺼져야 한다.
 *
 * **[scope] 는 메인 디스패처여야 한다.** 재생 포트 구현이 양 플랫폼 모두 메인 스레드 전용이다.
 */
class SecretModeRunner(
    private val playbackController: PlaybackController,
    private val startSecretModeUseCase: StartSecretModeUseCase,
    private val endSecretModeUseCase: EndSecretModeUseCase,
    private val fetchPlaylistsByTypeUseCase: FetchPlaylistsByTypeUseCase,
    private val fetchSecretAudioMediaIdListFlowUseCase: FetchSecretAudioMediaIdListFlowUseCase,
    private val clock: Clock = Clock.System,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {

    // 기기가 깊이 잠들면 delay 도 단조 시계도 멈춘다. 종료 시각은 벽시계로 두고 앱이 다시 보일 때
    // [endIfExpired] 로 한 번 더 본다.
    private var endsAt: Instant? = null
    private var job: Job? = null

    // 멈춰 둔 남은 시간. 멈춘 동안에도 화면은 이 값을 보여 준다 — null 로 두면 꺼진 것처럼 읽힌다.
    private var pausedRemaining: Duration? = null

    /** 꺼져 있으면 null. */
    val remaining: Duration?
        get() = pausedRemaining ?: endsAt?.let { (it - clock.now()).coerceAtLeast(Duration.ZERO) }

    fun start(total: Duration) {
        pausedRemaining = null
        job?.cancel()
        endsAt = clock.now() + total
        job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            startSecretModeUseCase()
            delay(total)
            finish()
        }
    }

    /**
     * 끊기면 안 되는 일이 도는 동안 시계를 멈춘다. 비밀모드가 꺼지면 화면 스택이 메인까지 걷히므로,
     * 그 일을 하던 화면도 함께 닫혀 버린다.
     *
     * **멈춘 채로 두면 비밀모드가 영영 켜져 있다.** 부르는 쪽은 성공·실패·취소 어느 쪽으로 끝나든
     * [resume] 해야 한다.
     */
    fun pause() {
        if (pausedRemaining != null) return
        val left = remaining ?: return
        job?.cancel()
        job = null
        endsAt = null
        pausedRemaining = left
    }

    fun resume() {
        val left = pausedRemaining ?: return
        pausedRemaining = null
        endsAt = clock.now() + left
        job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            delay(left)
            finish()
        }
    }

    fun endIfExpired() {
        val deadline = endsAt ?: return
        if (clock.now() >= deadline) end()
    }

    fun end() {
        job?.cancel()
        // 끄는 것을 미루지 않는다. 다음 프레임에 비밀 화면이 그려지기 전에 꺼져 있어야 한다.
        job = scope.launch(start = CoroutineStart.UNDISPATCHED) { finish() }
    }

    private suspend fun finish() {
        endsAt = null
        pausedRemaining = null
        endSecretModeUseCase()
        removeSecretTracksFromQueue()
    }

    // "전체" 큐에는 비밀모드 중에 비밀 플레이리스트로 옮긴 곡이 남아 있을 수 있다. 다른 플레이리스트에
    // 같이 든 곡은 거기서 보이는 곡이라 건드리지 않는다.
    private suspend fun removeSecretTracksFromQueue() {
        val playlistId = playbackController.currentPlaylistId ?: return
        if (isSecretPlaylist(playlistId)) {
            playbackController.clearQueue()
        } else if (isPlaylistOf(PlaylistType.TOTAL, playlistId)) {
            val secretIds = fetchSecretAudioMediaIdListFlowUseCase().first().toSet()
            // 듣던 곡을 빼면 빠진 자리를 다음 곡(비밀일 수 있다)이 이어받아 재생된다.
            if (playbackController.playingMediaItem?.mediaId?.toIntOrNull() in secretIds) {
                playbackController.clearQueue()
            } else {
                secretIds.forEach { playbackController.tryDeleteMediaItem(playlistId, it) }
            }
        }
    }

    // 비밀 플레이리스트와 비밀 찜 둘 다. 어느 쪽에서 틀어도 큐에 비밀 곡만 들어 있다.
    private suspend fun isSecretPlaylist(playlistId: Int): Boolean =
        PlaylistType.entries.filter { it.isSecret }.any { isPlaylistOf(it, playlistId) }

    // 목록에서 찾는다. 재생 중인 플레이리스트가 지워졌으면 단건 조회는 값을 내지 않는다.
    private suspend fun isPlaylistOf(type: PlaylistType, playlistId: Int): Boolean =
        fetchPlaylistsByTypeUseCase(type).first().any { it.id == playlistId }
}
