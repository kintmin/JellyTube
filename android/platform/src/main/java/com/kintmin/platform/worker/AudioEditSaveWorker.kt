package com.kintmin.platform.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kintmin.domain.audio_edit.model.AudioEditClip
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.usecase.ApplyAudioEditUseCase
import com.kintmin.domain.audio_edit.usecase.SaveEditedAudioAsNewMediaUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.platform.push_notification.PushNotificationIdGenerator
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.notifications.AudioEditSaveNotification
import com.kintmin.platform.push_notification.notifications.AudioEditSaveResultNotification
import com.kintmin.platform.service_controller.MediaControllerManager
import com.kintmin.platform.service_controller.mapper.toMediaControlData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AudioEditSaveWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val applyAudioEditUseCase: ApplyAudioEditUseCase by inject()
    private val saveEditedAudioAsNewMediaUseCase: SaveEditedAudioAsNewMediaUseCase by inject()
    private val mediaControllerManager: MediaControllerManager by inject()
    private val pushNotificationManager: PushNotificationManager by inject()
    private val appLog: AppLog by inject()

    /** 이번 저장의 진행 알림 번호. 동시에 저장하는 다른 음원과 겹치지 않게 저장마다 새로 뽑는다. */
    private val progressNotificationId = PushNotificationIdGenerator.random()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationData = AudioEditSaveNotification(id = progressNotificationId)
        val notification = notificationData.createNotification(appContext)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationData.id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationData.id, notification)
        }
    }

    override suspend fun doWork(): Result {
        val foregroundResult = runCatching { setForegroundAsync(getForegroundInfo()) }
        if (foregroundResult.isFailure) {
            val errorMessage = "포그라운드 서비스 시작 실패: ${foregroundResult.exceptionOrNull()?.message ?: "알 수 없는 오류"}"
            notifyFailure(errorMessage)
            return Result.failure()
        }

        val audioMediaId = inputData.getInt(INPUT_DATA_AUDIO_MEDIA_ID, -1)
        val timeline = inputData.readTimeline()
        if (audioMediaId == -1 || timeline == null) {
            notifyFailure("저장할 편집 정보를 읽지 못했습니다.")
            return Result.failure()
        }
        // 이름이 있으면 새 음원으로 저장, 없으면 덮어쓰기
        val newMediaName = inputData.getString(INPUT_DATA_NEW_MEDIA_NAME)

        val progressNotification = AudioEditSaveNotification(id = progressNotificationId)
        pushNotificationManager.sendNotification(progressNotification)

        val result = if (newMediaName == null) {
            applyAudioEditUseCase(audioMediaId = audioMediaId, timeline = timeline)
        } else {
            saveEditedAudioAsNewMediaUseCase(
                audioMediaId = audioMediaId,
                parts = timeline.toParts(),
                name = newMediaName,
            )
        }
        pushNotificationManager.cancelNotification(progressNotification.id)

        return result.fold(
            onSuccess = { audioMedia ->
                if (newMediaName == null) {
                    // 재생 큐는 같은 플레이리스트를 다시 재생할 때 갱신되지 않아서 직접 교체해야 편집본이 재생된다.
                    withContext(Dispatchers.Main) {
                        mediaControllerManager.tryReplaceMediaItem(
                            mediaId = audioMedia.id,
                            mediaItem = audioMedia.toMediaControlData(),
                        )
                    }
                }
                pushNotificationManager.sendNotification(
                    AudioEditSaveResultNotification(
                        resultType = if (newMediaName == null) {
                            AudioEditSaveResultNotification.ResultType.OverwriteSuccess
                        } else {
                            AudioEditSaveResultNotification.ResultType.NewMediaSuccess
                        },
                        contentText = audioMedia.name,
                        audioMediaId = audioMedia.id,
                    )
                )
                Result.success()
            },
            onFailure = { error ->
                notifyFailure(error.message.toString())
                Result.failure()
            },
        )
    }

    private suspend fun notifyFailure(errorMessage: String) {
        appLog.sendDebugLog(DebugLog("AudioEditSaveWorker", errorMessage))
        pushNotificationManager.sendNotification(
            AudioEditSaveResultNotification(
                resultType = AudioEditSaveResultNotification.ResultType.Failure,
                contentText = "저장에 실패했습니다. 로그를 확인해주세요.",
            )
        )
    }

    companion object {
        private const val INPUT_DATA_AUDIO_MEDIA_ID = "audio_media_id"
        private const val INPUT_DATA_NEW_MEDIA_NAME = "new_media_name"
        private const val INPUT_DATA_ORIGIN_DURATION_MS = "origin_duration_ms"
        private const val INPUT_DATA_END_MS = "end_ms"
        private const val INPUT_DATA_CLIP_SOURCE_START_MS = "clip_source_start_ms"
        private const val INPUT_DATA_CLIP_SOURCE_END_MS = "clip_source_end_ms"
        private const val INPUT_DATA_CLIP_POSITION_MS = "clip_position_ms"
        private const val INPUT_DATA_CLIP_IS_LINKED_TO_PREVIOUS = "clip_is_linked_to_previous"

        /**
         * WorkManager 입력 한도(10KB) 안에 들어가는 조각 수 상한.
         * 조각 하나가 Long 3개 + Boolean 1개(약 25바이트)라 400개 남짓까지 들어가지만, 키·이름 몫을 남겨 둔다.
         */
        const val MAX_CLIP_COUNT = 300

        /**
         * 저장 요청을 Worker 입력으로 만든다. [newMediaName]이 null이면 덮어쓰기다.
         *
         * 도메인 타임라인은 직렬화하지 않으므로 조각을 같은 길이의 숫자 배열들로 풀어 담는다.
         * JSON보다 작아 입력 한도에 덜 걸린다.
         */
        fun inputDataOf(audioMediaId: Int, timeline: AudioEditTimeline, newMediaName: String?): Data = workDataOf(
            INPUT_DATA_AUDIO_MEDIA_ID to audioMediaId,
            INPUT_DATA_NEW_MEDIA_NAME to newMediaName,
            INPUT_DATA_ORIGIN_DURATION_MS to timeline.originDurationMs,
            INPUT_DATA_END_MS to timeline.endMs,
            INPUT_DATA_CLIP_SOURCE_START_MS to timeline.clips.map { it.sourceStartMs }.toLongArray(),
            INPUT_DATA_CLIP_SOURCE_END_MS to timeline.clips.map { it.sourceEndMs }.toLongArray(),
            INPUT_DATA_CLIP_POSITION_MS to timeline.clips.map { it.positionMs }.toLongArray(),
            INPUT_DATA_CLIP_IS_LINKED_TO_PREVIOUS to timeline.clips.map { it.isLinkedToPrevious }.toBooleanArray(),
        )

        /** [inputDataOf]로 담은 타임라인을 되살린다. 배열이 빠졌거나 길이가 어긋나면 null. */
        private fun Data.readTimeline(): AudioEditTimeline? {
            val sourceStartMsList = getLongArray(INPUT_DATA_CLIP_SOURCE_START_MS) ?: return null
            val sourceEndMsList = getLongArray(INPUT_DATA_CLIP_SOURCE_END_MS) ?: return null
            val positionMsList = getLongArray(INPUT_DATA_CLIP_POSITION_MS) ?: return null
            val isLinkedList = getBooleanArray(INPUT_DATA_CLIP_IS_LINKED_TO_PREVIOUS) ?: return null
            val clipCount = sourceStartMsList.size
            if (listOf(sourceEndMsList.size, positionMsList.size, isLinkedList.size).any { it != clipCount }) return null

            return AudioEditTimeline(
                originDurationMs = getLong(INPUT_DATA_ORIGIN_DURATION_MS, -1L).takeIf { it > 0 } ?: return null,
                clips = List(clipCount) { index ->
                    AudioEditClip(
                        sourceStartMs = sourceStartMsList[index],
                        sourceEndMs = sourceEndMsList[index],
                        positionMs = positionMsList[index],
                        isLinkedToPrevious = isLinkedList[index],
                    )
                },
                endMs = getLong(INPUT_DATA_END_MS, -1L).takeIf { it > 0 } ?: return null,
            )
        }
    }
}
