package com.kintmin.jellytube.di

import com.kintmin.jellytube.worker.AnomalyDataCheckWorker
import com.kintmin.jellytube.worker.AudioEditSaveWorker
import com.kintmin.jellytube.worker.DailyBackupWorker
import com.kintmin.jellytube.worker.DailyResetImmediatelyWorker
import com.kintmin.jellytube.worker.LyricsVariantWorker
import com.kintmin.jellytube.worker.YoutubeDownloadWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.Module
import org.koin.dsl.module

val workerModule: Module = module {
    workerOf(::YoutubeDownloadWorker)
    workerOf(::DailyResetImmediatelyWorker)
    workerOf(::AnomalyDataCheckWorker)
    workerOf(::LyricsVariantWorker)
    workerOf(::AudioEditSaveWorker)
    workerOf(::DailyBackupWorker)
}
