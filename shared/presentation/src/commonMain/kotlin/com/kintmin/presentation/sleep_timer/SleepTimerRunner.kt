package com.kintmin.presentation.sleep_timer

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_play_setting.SleepTimerFadeOut
import com.kintmin.domain.audio_play_setting.SleepTimerSchedule
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.presentation.port.AudioPreviewPlayer
import com.kintmin.presentation.port.PlaybackController
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
 * **화면이 아니라 앱에 하나만 있어야 한다.** 화면에 매달면 화면을 떠나는 순간 타이머가 죽는다.
 * 앱에 하나로 두면 재생이 프로세스를 살려 두는 동안 같이 살아 있다.
 *
 * **[scope] 는 메인 디스패처여야 한다.** 재생 포트 구현이 양 플랫폼 모두 메인 스레드 전용이다.
 */
class SleepTimerRunner(
    private val playbackController: PlaybackController,
    private val announcementPlayer: AudioPreviewPlayer,
    private val appLog: AppLog,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {

    private var schedule: SleepTimerSchedule? = null
    private var job: Job? = null

    /** 켜져 있지 않으면 null. */
    val remaining: Duration?
        get() = schedule?.remaining()

    /** 멈춘 뒤에 들려줄 음성. 없으면 들려주지 않는다. */
    var announcement: AudioMedia? = null

    fun start(total: Duration) {
        job?.cancel()
        log("잠들기 타이머 켬: ${total.inWholeMinutes}분")

        val started = SleepTimerSchedule(timeSource.markNow(), total)
        schedule = started
        job = scope.launch {
            try {
                delay(started.delayUntilFadeOut())
                log("잠들기 타이머 끝 ${SleepTimerFadeOut.DURATION.inWholeSeconds}초 전 페이드아웃 시작: 재생 중=${playbackController.isPlaying}")
                fadeOutAndPause()
                log("잠들기 타이머가 재생을 멈춤")
                clearSchedule(started)
                announcement?.let { playAnnouncement(it) }
            } finally {
                clearSchedule(started)
            }
        }
    }

    // 앞선 타이머가 늦게 취소되면서 새로 켠 타이머를 지우지 않게 한다.
    private fun clearSchedule(started: SleepTimerSchedule) {
        if (schedule === started) schedule = null
    }

    fun cancel() {
        if (schedule != null) log("잠들기 타이머 끔: 사용자")
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
            playbackController.setPlaybackVolume(1f)
        }
    }

    /**
     * 메인 재생을 멈춘 **뒤에** 부른다. 그래야 아래 폴링이 "메인 재생이 다시 시작됐다" 를 볼 수 있다.
     */
    private suspend fun playAnnouncement(announcement: AudioMedia) {
        val duration = announcement.playbackDuration
            ?: return log("안내 음성 길이를 몰라 건너뜀: id=${announcement.id}")
        log("안내 음성 재생: id=${announcement.id}")

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

    private fun log(message: String) = appLog.sendDebugLog(DebugLog("SleepTimer", message))

    private companion object {

        /** 저장된 길이가 실제 파일보다 조금 짧을 때 끝을 자르지 않을 만큼의 여유. */
        val ANNOUNCEMENT_TAIL_MARGIN = 500.milliseconds

        val ANNOUNCEMENT_POLL_INTERVAL = 200.milliseconds
    }
}
