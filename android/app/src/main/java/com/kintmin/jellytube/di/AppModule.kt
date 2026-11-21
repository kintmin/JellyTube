package com.kintmin.jellytube.di

import com.kintmin.data.python_bridge.LyricsTransliterator
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.domain.step.ScheduleDailyStepClosing
import com.kintmin.jellytube.audio_preview.AudioPreviewPlayerImpl
import com.kintmin.jellytube.data_migration.NsdMigrationSourceDiscovery
import com.kintmin.jellytube.log_impl.AppLogImpl
import com.kintmin.jellytube.push_notification.PushNotificationManager
import com.kintmin.jellytube.push_notification.PushNotificationManagerImpl
import com.kintmin.jellytube.python_bridge_impl.LyricsTransliteratorImpl
import com.kintmin.jellytube.python_bridge_impl.PythonExecutorImpl
import com.kintmin.jellytube.service_controller.DataMigrationSendControllerImpl
import com.kintmin.jellytube.service_controller.FileShareServerControllerImpl
import com.kintmin.jellytube.service_controller.MigrationReceiveKeepAliveImpl
import com.kintmin.jellytube.service_controller.RestoreKeepAliveImpl
import com.kintmin.jellytube.service_controller.PlaybackControllerImpl
import com.kintmin.jellytube.service_controller.StepMeasurementControllerImpl
import com.kintmin.jellytube.worker.RunningTaskMonitorImpl
import com.kintmin.jellytube.worker.usecase.StartAnomalyDataCheckImpl
import com.kintmin.jellytube.worker.usecase.StartAudioEditSaveImpl
import com.kintmin.jellytube.worker.usecase.StartLyricsVariantCreationImpl
import com.kintmin.jellytube.worker.usecase.StartYoutubeDownloadImpl
import com.kintmin.jellytube.worker.usecase.ScheduleDailyStepClosingImpl
import com.kintmin.log.AppLog
import com.kintmin.presentation.port.AudioPreviewPlayer
import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.StartAnomalyDataCheck
import com.kintmin.presentation.port.StartAudioEditSave
import com.kintmin.presentation.port.StartLyricsVariantCreation
import com.kintmin.presentation.port.StartYoutubeDownload
import com.kintmin.presentation.port.FileShareServerController
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.port.MigrationReceiveKeepAlive
import com.kintmin.presentation.port.RestoreKeepAlive
import com.kintmin.presentation.port.MigrationSourceDiscovery
import com.kintmin.presentation.port.RunningTaskMonitor
import com.kintmin.presentation.port.StepMeasurementController
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Worker 바인딩만 [workerModule] 로 나눠 둔다. `workerOf` 는 `workManagerFactory()` 가 쓰는
 * 별도 해석 경로라 `single` 과 성격이 다르다.
 */
val appModule: Module = module {
    singleOf(::AppLogImpl) bind AppLog::class

    single<PythonExecutor> { PythonExecutorImpl(androidContext()) }
    single<LyricsTransliterator> { LyricsTransliteratorImpl(androidContext()) }

    single<PushNotificationManager> { PushNotificationManagerImpl(androidContext()) }
    single<PlaybackController> { PlaybackControllerImpl(androidContext()) }
    single<AudioPreviewPlayer> { AudioPreviewPlayerImpl(androidContext()) }
    single<StepMeasurementController> { StepMeasurementControllerImpl(androidContext()) }
    single<FileShareServerController> { FileShareServerControllerImpl(androidContext(), get(), get(), get(), get()) }
    single<ScheduleDailyStepClosing> { ScheduleDailyStepClosingImpl(androidContext()) }
    single<StartYoutubeDownload> { StartYoutubeDownloadImpl(androidContext()) }
    single<StartAnomalyDataCheck> { StartAnomalyDataCheckImpl(androidContext()) }
    single<StartLyricsVariantCreation> { StartLyricsVariantCreationImpl(androidContext()) }
    single<StartAudioEditSave> { StartAudioEditSaveImpl(androidContext()) }
    single<MigrationSourceDiscovery> { NsdMigrationSourceDiscovery(androidContext()) }
    single<MigrationReceiveKeepAlive> { MigrationReceiveKeepAliveImpl(androidContext()) }
    single<RestoreKeepAlive> { RestoreKeepAliveImpl(androidContext()) }
    single<DataMigrationSendController> { DataMigrationSendControllerImpl(androidContext(), get(), get(), get()) }
    single<RunningTaskMonitor> { RunningTaskMonitorImpl(androidContext(), get(), get()) }
}
