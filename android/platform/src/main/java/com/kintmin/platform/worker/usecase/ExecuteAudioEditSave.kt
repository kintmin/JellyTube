package com.kintmin.platform.worker.usecase

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.platform.worker.AudioEditSaveWorker

class ExecuteAudioEditSave(
    private val appContext: Context,
) {

    /** 편집본을 만들어 이 음원에 적용한다. */
    fun overwrite(audioMediaId: Int, timeline: AudioEditTimeline): Result<Unit> =
        enqueue(audioMediaId = audioMediaId, timeline = timeline, newMediaName = null)

    /** 편집본을 원본과 별개인 새 음원으로 저장한다. */
    fun saveAsNewMedia(audioMediaId: Int, timeline: AudioEditTimeline, name: String): Result<Unit> =
        enqueue(audioMediaId = audioMediaId, timeline = timeline, newMediaName = name)

    private fun enqueue(audioMediaId: Int, timeline: AudioEditTimeline, newMediaName: String?): Result<Unit> {
        if (timeline.clips.size > AudioEditSaveWorker.MAX_CLIP_COUNT) {
            return Result.failure(AudioEditTooManyClips())
        }

        return runCatching {
            val inputData: Data = AudioEditSaveWorker.inputDataOf(
                audioMediaId = audioMediaId,
                timeline = timeline,
                newMediaName = newMediaName,
            )
            val request = OneTimeWorkRequestBuilder<AudioEditSaveWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(inputData)
                .build()
            WorkManager.getInstance(appContext).enqueue(request)
            Unit
        }
    }
}

class AudioEditTooManyClips(
    override val message: String = "조각이 너무 많아 저장할 수 없습니다. 조각을 병합하거나 지운 뒤 다시 시도해주세요."
) : Throwable()
