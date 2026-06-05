package com.kintmin.platform.worker.di

import com.kintmin.platform.worker.DailyResetImmediatelyWorker
import com.kintmin.platform.worker.LoadBalancedDailyResetWorker
import com.kintmin.platform.worker.YoutubeDownloadWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.Module
import org.koin.dsl.module

val workerModule: Module = module {
    workerOf(::YoutubeDownloadWorker)
    workerOf(::LoadBalancedDailyResetWorker)
    workerOf(::DailyResetImmediatelyWorker)
}
