package com.kintmin.presentation.secret_mode

import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistsByTypeUseCase
import com.kintmin.domain.secret_mode.usecase.EndSecretModeUseCase
import com.kintmin.domain.secret_mode.usecase.FetchSecretTrackListFlowUseCase
import com.kintmin.domain.secret_mode.usecase.StartSecretModeUseCase
import com.kintmin.presentation.port.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

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
    private val fetchSecretTrackListFlowUseCase: FetchSecretTrackListFlowUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {

    private var endMark: TimeMark? = null
    private var job: Job? = null

    /** 꺼져 있으면 null. */
    val remaining: Duration?
        get() = endMark?.let { -it.elapsedNow() }?.coerceAtLeast(Duration.ZERO)

    fun start(total: Duration) {
        job?.cancel()
        startSecretModeUseCase()
        endMark = TimeSource.Monotonic.markNow() + total
        job = scope.launch {
            delay(total)
            finish()
        }
    }

    fun end() {
        job?.cancel()
        job = scope.launch { finish() }
    }

    private suspend fun finish() {
        endMark = null
        endSecretModeUseCase()
        removeSecretTracksFromQueue()
    }

    // "전체" 큐에는 비밀모드 중에 비밀 플레이리스트로 옮긴 곡이 남아 있을 수 있다. 다른 플레이리스트에
    // 같이 든 곡은 거기서 보이는 곡이라 건드리지 않는다.
    private suspend fun removeSecretTracksFromQueue() {
        val playlistId = playbackController.currentPlaylistId ?: return
        if (isPlaylistOf(PlaylistType.SECRET, playlistId)) {
            playbackController.clearQueue()
        } else if (isPlaylistOf(PlaylistType.TOTAL, playlistId)) {
            fetchSecretTrackListFlowUseCase().first().forEach {
                playbackController.tryDeleteMediaItem(playlistId, it.audioMediaId)
            }
        }
    }

    // 목록에서 찾는다. 재생 중인 플레이리스트가 지워졌으면 단건 조회는 값을 내지 않는다.
    private suspend fun isPlaylistOf(type: PlaylistType, playlistId: Int): Boolean =
        fetchPlaylistsByTypeUseCase(type).first().any { it.id == playlistId }
}
