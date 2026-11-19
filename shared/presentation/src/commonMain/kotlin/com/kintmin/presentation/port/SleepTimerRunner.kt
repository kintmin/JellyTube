package com.kintmin.presentation.port

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_play_setting.SleepTimerFadeOut
import com.kintmin.domain.audio_play_setting.SleepTimerSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * 정해 둔 시간이 지나면 페이드아웃하고 재생을 멈춘다. 마무리 음성을 골라 두면 멈춘 뒤에 들려준다.
 *
 * **화면이 아니라 앱에 하나만 있어야 한다.** 재생 상태 폴링은 화면이 보일 때만 돌아서 여기 얹으면
 * 화면을 떠나는 순간 타이머가 죽는다. 앱에 하나로 두면 재생이 프로세스를 살려 두는 동안 같이
 * 살아 있고, 프로세스가 죽었다면 재생도 이미 죽어 있다.
 *
 * **[scope] 는 메인 디스패처여야 한다.** 재생 포트 구현이 양 플랫폼 모두 메인 스레드 전용이다.
 */
class SleepTimerRunner(
    private val playbackController: PlaybackController,
    private val announcementPlayer: AudioPreviewPlayer,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {

    private var schedule: SleepTimerSchedule? = null
    private var job: Job? = null

    /** 켜져 있지 않으면 null. 화면이 재생 상태를 긁는 주기에 함께 읽는다. */
    val remaining: Duration?
        get() = schedule?.remaining()

    /**
     * 멈춘 뒤에 들려줄 음성. 없으면 들려주지 않는다.
     *
     * 타이머가 도는 중에 바꿔도 **울릴 때 골라져 있는 것**이 난다 — 켤 때 잡아 두면 바꾼 것이
     * 조용히 무시된다.
     */
    var announcement: AudioMedia? = null

    /** 곡이 바뀌어도 이 타이머는 그대로다. 끄는 것은 [cancel] 뿐이다. */
    fun start(total: Duration) {
        cancel()

        val started = SleepTimerSchedule(timeSource.markNow(), total)
        schedule = started
        job = scope.launch {
            try {
                delay(started.delayUntilFadeOut())
                fadeOutAndPause()
                announcement?.let { playAnnouncement(it) }
            } finally {
                // 앞선 타이머가 늦게 취소되면서 새로 켠 타이머를 지우지 않게 한다.
                if (schedule === started) schedule = null
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        schedule = null
    }

    private suspend fun fadeOutAndPause() {
        try {
            repeat(SleepTimerFadeOut.TICK_COUNT) { tick ->
                val progress = (tick + 1).toFloat() / SleepTimerFadeOut.TICK_COUNT
                playbackController.setPlaybackVolume(SleepTimerFadeOut.volumeAt(progress))
                delay(SleepTimerFadeOut.TICK)
            }
            playbackController.pause()
        } finally {
            // 깎은 볼륨을 남기면 다음 재생이 영구히 무음이 된다. 도중에 꺼져도 되돌린다.
            playbackController.setPlaybackVolume(1f)
        }
    }

    /**
     * 메인 재생을 멈춘 **뒤에** 부른다. 그래야 아래 폴링이 "메인 재생이 다시 시작됐다" 를 볼 수 있다.
     */
    private suspend fun playAnnouncement(announcement: AudioMedia) {
        val duration = announcement.playbackDuration ?: return

        announcementPlayer.play(
            audioFileFullPath = announcement.playbackFileFullPath,
            parts = listOf(AudioEditPart.Audio(startMs = 0, endMs = duration.inWholeMilliseconds)),
            startAtMs = 0,
        )
        try {
            withTimeoutOrNull(duration + ANNOUNCEMENT_TAIL_MARGIN) {
                // 다른 음원을 누르면 즉시 끊는다. 알림·제어센터·이어폰 명령은 공통 코드를 지나지
                // 않으므로 호출을 가로채는 대신 재생 상태를 본다.
                while (!playbackController.isPlaying) delay(ANNOUNCEMENT_POLL_INTERVAL)
            }
        } finally {
            announcementPlayer.release()
        }
    }

    private companion object {

        /** 저장된 길이가 실제 파일보다 조금 짧을 때 끝을 자르지 않을 만큼의 여유. */
        val ANNOUNCEMENT_TAIL_MARGIN = 500.milliseconds

        val ANNOUNCEMENT_POLL_INTERVAL = 200.milliseconds
    }
}
