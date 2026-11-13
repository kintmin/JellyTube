package com.kintmin.platform.di

import com.kintmin.domain.step.worker.RegisterDailyResetImmediatelyWorker
import com.kintmin.domain.step.worker.RegisterLoadBalancedDailyResetWorker
import com.kintmin.platform.audio_preview.AudioPreviewPlayerImpl
import com.kintmin.platform.data_migration.UnsupportedMigrationSourceDiscovery
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.PushNotificationManagerImpl
import com.kintmin.platform.running_work.RunningWorkMonitorImpl
import com.kintmin.platform.service_controller.DataMigrationSendControllerImpl
import com.kintmin.platform.service_controller.FileShareServerControllerImpl
import com.kintmin.platform.service_controller.MediaControllerManagerImpl
import com.kintmin.platform.service_controller.StepMeasurementControllerImpl
import com.kintmin.platform.worker.usecase.ExecuteAnomalyDataCheckImpl
import com.kintmin.platform.worker.usecase.ExecuteAudioEditSaveImpl
import com.kintmin.platform.worker.usecase.ExecuteLyricsVariantCreationImpl
import com.kintmin.platform.worker.usecase.ExecuteYoutubeDownloadImpl
import com.kintmin.platform.worker.usecase.RegisterDailyResetImmediatelyWorkerImpl
import com.kintmin.platform.worker.usecase.RegisterLoadBalancedDailyResetWorkerImpl
import com.kintmin.presentation.port.AudioPreviewPlayer
import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.ExecuteAnomalyDataCheck
import com.kintmin.presentation.port.ExecuteAudioEditSave
import com.kintmin.presentation.port.ExecuteLyricsVariantCreation
import com.kintmin.presentation.port.ExecuteYoutubeDownload
import com.kintmin.presentation.port.FileShareServerController
import com.kintmin.presentation.port.MediaControllerManager
import com.kintmin.presentation.port.MigrationSourceDiscovery
import com.kintmin.presentation.port.RunningWorkMonitor
import com.kintmin.presentation.port.StepMeasurementController
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val platformModule: Module = module {
    single<PushNotificationManager> { PushNotificationManagerImpl(androidContext()) }
    single<MediaControllerManager> { MediaControllerManagerImpl(androidContext()) }
    single<AudioPreviewPlayer> { AudioPreviewPlayerImpl(androidContext()) }
    single<StepMeasurementController> { StepMeasurementControllerImpl(androidContext()) }
    single<FileShareServerController> { FileShareServerControllerImpl(androidContext(), get(), get(), get(), get()) }
    single<RegisterDailyResetImmediatelyWorker> {
        RegisterDailyResetImmediatelyWorkerImpl(androidContext())
    }
    single<RegisterLoadBalancedDailyResetWorker> {
        RegisterLoadBalancedDailyResetWorkerImpl(androidContext(), get())
    }
    single<ExecuteYoutubeDownload> { ExecuteYoutubeDownloadImpl(androidContext()) }
    single<ExecuteAnomalyDataCheck> { ExecuteAnomalyDataCheckImpl(androidContext()) }
    single<ExecuteLyricsVariantCreation> { ExecuteLyricsVariantCreationImpl(androidContext()) }
    single<ExecuteAudioEditSave> { ExecuteAudioEditSaveImpl(androidContext()) }
    single<MigrationSourceDiscovery> { UnsupportedMigrationSourceDiscovery() }
    single<DataMigrationSendController> { DataMigrationSendControllerImpl(androidContext(), get(), get(), get()) }
    single<RunningWorkMonitor> { RunningWorkMonitorImpl(androidContext(), get(), get()) }
}
