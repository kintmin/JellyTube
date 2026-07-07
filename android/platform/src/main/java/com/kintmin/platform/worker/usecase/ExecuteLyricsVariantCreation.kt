package com.kintmin.platform.worker.usecase

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.platform.worker.LyricsVariantWorker

class ExecuteLyricsVariantCreation(
    private val appContext: Context,
) {

    operator fun invoke(
        audioMediaId: Int,
        lyricFileFullPath: String,
        variant: LyricsVariant,
        sourceLanguage: String,
    ) {
        val request = OneTimeWorkRequestBuilder<LyricsVariantWorker>()
            .setInputData(
                workDataOf(
                    LyricsVariantWorker.INPUT_DATA_AUDIO_MEDIA_ID to audioMediaId,
                    LyricsVariantWorker.INPUT_DATA_LYRIC_FILE_FULL_PATH to lyricFileFullPath,
                    LyricsVariantWorker.INPUT_DATA_VARIANT to variant.name,
                    LyricsVariantWorker.INPUT_DATA_SOURCE_LANGUAGE to sourceLanguage,
                )
            )
            .build()
        WorkManager.getInstance(appContext).enqueue(request)
    }
}
