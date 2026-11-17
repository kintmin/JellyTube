package com.kintmin.jellytube.worker.usecase

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.jellytube.worker.LyricsVariantWorker
import com.kintmin.presentation.port.StartLyricsVariantCreation

class StartLyricsVariantCreationImpl(
    private val appContext: Context,
) : StartLyricsVariantCreation {

    override operator fun invoke(
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
            .addTag(TAG)
            .build()
        WorkManager.getInstance(appContext).enqueue(request)
    }

    companion object {
        const val TAG = "lyrics_variant"
    }
}
