package com.kintmin.platform.worker.usecase

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.platform.worker.AudioEditSaveWorker

/**
 * 편집본 저장을 백그라운드 작업으로 맡긴다. 렌더가 수십 초 걸려 화면에서 기다리지 않는다.
 *
 * 저장 중인지는 AudioEditRepository의 저장 중 목록으로 확인한다. 같은 음원은 저장하는 쪽에서 막는다.
 */
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

/** 조각이 너무 많아 백그라운드 작업 입력 한도를 넘는다. */
class AudioEditTooManyClips(
    override val message: String = "조각이 너무 많아 저장할 수 없습니다. 조각을 병합하거나 지운 뒤 다시 시도해주세요."
) : Throwable()
