package com.kintmin.presentation.port

import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.usecase.ApplyAudioEditUseCase
import com.kintmin.domain.audio_edit.usecase.ReleaseAudioEditSaveUseCase
import com.kintmin.domain.audio_edit.usecase.SaveEditedAudioAsNewMediaUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.presentation.port.model.toMediaControlData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 앱 수명 스코프에서 렌더하므로 앱이 내려가면 유예 시간 뒤 멈춘다. 진행 중인 작업 수는
 * [IosRunningWorkMonitor] 가 저장 중 표시로 세므로 [IosRunningWorkTracker] 로 감싸지 않는다(두 번 센다).
 */
internal class IosExecuteAudioEditSave(
    private val applyAudioEditUseCase: ApplyAudioEditUseCase,
    private val saveEditedAudioAsNewMediaUseCase: SaveEditedAudioAsNewMediaUseCase,
    private val releaseAudioEditSaveUseCase: ReleaseAudioEditSaveUseCase,
    private val mediaControllerManager: MediaControllerManager,
    private val appLog: AppLog,
) : ExecuteAudioEditSave {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun overwrite(audioMediaId: Int, timeline: AudioEditTimeline): Result<Unit> = launchSave(audioMediaId) {
        applyAudioEditUseCase(audioMediaId = audioMediaId, timeline = timeline).onSuccess { audioMedia ->
            // 재생 큐는 같은 플레이리스트를 다시 재생할 때 갱신되지 않아서 직접 교체해야 편집본이 재생된다
            mediaControllerManager.tryReplaceMediaItem(
                mediaId = audioMedia.id,
                mediaItem = audioMedia.toMediaControlData(),
            )
            IosLocalNotification.show(WorkResultNotice.AudioEditSave.OverwriteSuccess(audioMedia.name, audioMedia.id))
        }
    }

    override fun saveAsNewMedia(audioMediaId: Int, timeline: AudioEditTimeline, name: String): Result<Unit> =
        launchSave(audioMediaId) {
            saveEditedAudioAsNewMediaUseCase(
                audioMediaId = audioMediaId,
                parts = timeline.toParts(),
                name = name,
            ).onSuccess { audioMedia ->
                IosLocalNotification.show(WorkResultNotice.AudioEditSave.NewMediaSuccess(audioMedia.name, audioMedia.id))
            }
        }

    private fun launchSave(audioMediaId: Int, save: suspend () -> Result<*>): Result<Unit> {
        scope.launch {
            // 이 스코프에는 예외 처리기가 없어 새는 예외는 앱을 죽인다
            val result = try {
                withBackgroundExecutionWindow("AudioEditSave") { save() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Result.failure<Unit>(e)
            }

            result.onFailure { throwable ->
                // Repository 에 닿기 전에 실패하면 화면이 건 저장 중 예약이 남는다
                releaseAudioEditSaveUseCase(audioMediaId)
                appLog.sendDebugLog(DebugLog("IosExecuteAudioEditSave", throwable.message ?: "알 수 없는 오류"))
                IosLocalNotification.show(WorkResultNotice.AudioEditSave.Failure)
            }
        }
        return Result.success(Unit)
    }
}
