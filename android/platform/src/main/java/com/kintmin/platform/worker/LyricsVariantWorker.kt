package com.kintmin.platform.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.CreateLyricsVariantUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.notifications.LyricsVariantResultNotification
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 원본 가사로부터 번역/음차 변형 파일을 백그라운드에서 생성하고,
 * 완료/실패 시 알림을 보낸다. (성공 알림 탭 시 해당 가사 화면으로 딥링크)
 */
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
        val sourceLanguage = inputData.getString(INPUT_DATA_SOURCE_LANGUAGE) ?: "en"

        if (audioMediaId == -1 || lyricFileFullPath == null || variantName == null) {
            return Result.failure()
        }
        val variant = runCatching { LyricsVariant.valueOf(variantName) }.getOrNull()
            ?: return Result.failure()

        return createLyricsVariantUseCase(lyricFileFullPath, variant, sourceLanguage).fold(
            onSuccess = {
                pushNotificationManager.sendNotification(
                    LyricsVariantResultNotification(
                        variant = variant,
                        resultType = LyricsVariantResultNotification.ResultType.Success,
                        contentText = "탭하여 결과를 확인하세요.",
                        audioMediaId = audioMediaId,
                    )
                )
                Result.success()
            },
            onFailure = { error ->
                appLog.sendDebugLog(DebugLog("LyricsVariantWorker", error.message.toString()))
                pushNotificationManager.sendNotification(
                    LyricsVariantResultNotification(
                        variant = variant,
                        resultType = LyricsVariantResultNotification.ResultType.Failure,
                        contentText = "번역에 실패했습니다. 로그를 확인해주세요.",
                    )
                )
                Result.failure()
            },
        )
    }

    companion object {
        const val INPUT_DATA_AUDIO_MEDIA_ID = "audio_media_id"
        const val INPUT_DATA_LYRIC_FILE_FULL_PATH = "lyric_file_full_path"
        const val INPUT_DATA_VARIANT = "variant"
        const val INPUT_DATA_SOURCE_LANGUAGE = "source_language"
    }
}
