package com.kintmin.jellytube.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.CreateLyricsVariantUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.jellytube.push_notification.PushNotificationManager
import com.kintmin.jellytube.push_notification.notifications.LyricsVariantResultNotification
import com.kintmin.presentation.port.model.WorkResultNotice
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LyricsVariantWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val createLyricsVariantUseCase: CreateLyricsVariantUseCase by inject()
    private val pushNotificationManager: PushNotificationManager by inject()
    private val appLog: AppLog by inject()

    override suspend fun doWork(): Result {
        val audioMediaId = inputData.getInt(INPUT_DATA_AUDIO_MEDIA_ID, -1)
        val lyricFileFullPath = inputData.getString(INPUT_DATA_LYRIC_FILE_FULL_PATH)
        val variantName = inputData.getString(INPUT_DATA_VARIANT)

        if (audioMediaId == -1 || lyricFileFullPath == null || variantName == null) {
            return Result.failure()
        }
        val variant = suspendRunCatching { LyricsVariant.valueOf(variantName) }.getOrNull()
            ?: return Result.failure()

        return createLyricsVariantUseCase(lyricFileFullPath, variant).fold(
            onSuccess = {
                pushNotificationManager.sendNotification(
                    LyricsVariantResultNotification(
                        WorkResultNotice.LyricsVariantCreation.Success(variant, audioMediaId),
                    )
                )
                Result.success()
            },
            onFailure = { error ->
                appLog.sendErrorLog(ErrorLog("LyricsVariantWorker", "가사 변형 만들기 실패", error))
                pushNotificationManager.sendNotification(
                    LyricsVariantResultNotification(WorkResultNotice.LyricsVariantCreation.Failure(variant)),
                )
                Result.failure()
            },
        )
    }

    companion object {
        const val INPUT_DATA_AUDIO_MEDIA_ID = "audio_media_id"
        const val INPUT_DATA_LYRIC_FILE_FULL_PATH = "lyric_file_full_path"
        const val INPUT_DATA_VARIANT = "variant"
    }
}
