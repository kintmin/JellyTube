package com.kintmin.jellytube.di

import com.kintmin.data.python_bridge.LyricsTransliterator
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.domain.step.ScheduleDailyStepClosing
import com.kintmin.jellytube.audio_preview.AudioPreviewPlayerImpl
import com.kintmin.jellytube.data_migration.UnsupportedMigrationSourceDiscovery
import com.kintmin.jellytube.log_impl.AppLogImpl
import com.kintmin.jellytube.push_notification.PushNotificationManager
import com.kintmin.jellytube.push_notification.PushNotificationManagerImpl
import com.kintmin.jellytube.python_bridge_impl.LyricsTransliteratorImpl
import com.kintmin.jellytube.python_bridge_impl.PythonExecutorImpl
import com.kintmin.jellytube.service_controller.DataMigrationSendControllerImpl
import com.kintmin.jellytube.service_controller.FileShareServerControllerImpl
import com.kintmin.jellytube.service_controller.MediaControllerManagerImpl
import com.kintmin.jellytube.service_controller.StepMeasurementControllerImpl
import com.kintmin.jellytube.worker.RunningWorkMonitorImpl
import com.kintmin.jellytube.worker.usecase.ExecuteAnomalyDataCheckImpl
import com.kintmin.jellytube.worker.usecase.ExecuteAudioEditSaveImpl
import com.kintmin.jellytube.worker.usecase.ExecuteLyricsVariantCreationImpl
import com.kintmin.jellytube.worker.usecase.ExecuteYoutubeDownloadImpl
import com.kintmin.jellytube.worker.usecase.ScheduleDailyStepClosingImpl
import com.kintmin.log.AppLog
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
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Composition Root. Android 런타임에 묶인 구현을 전부 여기서 인터페이스에 건다.
 *
 * Worker 바인딩만 [workerModule] 로 나눠 둔다. `workerOf` 는 `workManagerFactory()` 가 쓰는
 * 별도 해석 경로라 `single` 과 성격이 다르다.
 */
val appModule: Module = module {
    singleOf(::AppLogImpl) bind AppLog::class

    single<PythonExecutor> { PythonExecutorImpl(androidContext()) }
    single<LyricsTransliterator> { LyricsTransliteratorImpl(androidContext()) }

    single<PushNotificationManager> { PushNotificationManagerImpl(androidContext()) }
    single<MediaControllerManager> { MediaControllerManagerImpl(androidContext()) }
    single<AudioPreviewPlayer> { AudioPreviewPlayerImpl(androidContext()) }
    single<StepMeasurementController> { StepMeasurementControllerImpl(androidContext()) }
    single<FileShareServerController> { FileShareServerControllerImpl(androidContext(), get(), get(), get(), get()) }
    single<ScheduleDailyStepClosing> { ScheduleDailyStepClosingImpl(androidContext()) }
    single<ExecuteYoutubeDownload> { ExecuteYoutubeDownloadImpl(androidContext()) }
    single<ExecuteAnomalyDataCheck> { ExecuteAnomalyDataCheckImpl(androidContext()) }
    single<ExecuteLyricsVariantCreation> { ExecuteLyricsVariantCreationImpl(androidContext()) }
    single<ExecuteAudioEditSave> { ExecuteAudioEditSaveImpl(androidContext()) }
    single<MigrationSourceDiscovery> { UnsupportedMigrationSourceDiscovery() }
    single<DataMigrationSendController> { DataMigrationSendControllerImpl(androidContext(), get(), get(), get()) }
    single<RunningWorkMonitor> { RunningWorkMonitorImpl(androidContext(), get(), get()) }
}
