package com.kintmin.ios.adapter

import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.usecase.ApplyAudioEditUseCase
import com.kintmin.domain.audio_edit.usecase.SaveEditedAudioAsNewMediaUseCase
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.ios.notification.IosLocalNotification
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.presentation.platform.continueInBackgroundWhile
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.port.StartAudioEditSave
import com.kintmin.presentation.port.completeAudioEditSave
import com.kintmin.presentation.port.model.WorkResultNotice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 앱 수명 스코프에서 렌더하므로 앱이 내려가면 유예 시간 뒤 멈춘다. 진행 중인 작업 수는 저장 중
 * 표시로 이미 세고 있어 작업 카운터로 감싸지 않는다 — 감싸면 두 번 센다.
 */
internal class IosStartAudioEditSave(
    private val applyAudioEditUseCase: ApplyAudioEditUseCase,
    private val saveEditedAudioAsNewMediaUseCase: SaveEditedAudioAsNewMediaUseCase,
    private val mediaControllerManager: PlaybackController,
    private val appLog: AppLog,
) : StartAudioEditSave {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun overwrite(audioMediaId: Int, timeline: AudioEditTimeline): Result<Unit> =
        launchSave(isOverwrite = true) { applyAudioEditUseCase(audioMediaId = audioMediaId, timeline = timeline) }

    override fun saveAsNewMedia(audioMediaId: Int, timeline: AudioEditTimeline, name: String): Result<Unit> =
        launchSave(isOverwrite = false) {
            saveEditedAudioAsNewMediaUseCase(audioMediaId = audioMediaId, parts = timeline.toParts(), name = name)
        }

    private fun launchSave(isOverwrite: Boolean, save: suspend () -> Result<AudioMedia>): Result<Unit> {
        scope.launch {
            // 이 스코프에는 예외 처리기가 없어 새는 예외는 앱을 죽인다
            val result = try {
                continueInBackgroundWhile { save() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Result.failure(e)
            }

            result
                .onSuccess { audioMedia ->
                    runCatching { IosLocalNotification.show(mediaControllerManager.completeAudioEditSave(audioMedia, isOverwrite)) }
                        .onFailure { log(it) }
                }
                .onFailure { throwable ->
                    log(throwable)
                    IosLocalNotification.show(WorkResultNotice.AudioEditSave.Failure)
                }
        }
        return Result.success(Unit)
    }

    private suspend fun log(throwable: Throwable) {
        appLog.sendErrorLog(ErrorLog("IosStartAudioEditSave", "편집본 저장 실패", throwable))
    }
}
