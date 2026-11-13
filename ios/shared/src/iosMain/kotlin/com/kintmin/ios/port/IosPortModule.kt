package com.kintmin.ios.port

import com.kintmin.domain.file_share.ReceiveKeepAlive
import com.kintmin.presentation.port.AudioPreviewPlayer
import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.ExecuteAnomalyDataCheck
import com.kintmin.presentation.port.ExecuteAudioEditSave
import com.kintmin.presentation.port.ExecuteLyricsVariantCreation
import com.kintmin.presentation.port.ExecuteYoutubeDownload
import com.kintmin.presentation.port.FileShareServerController
import com.kintmin.presentation.port.RunningWorkMonitor
import com.kintmin.presentation.port.StepMeasurementController
import org.koin.core.module.Module
import org.koin.dsl.module

/** `presentation/port` 의 iOS 구현 바인딩. Android 쪽 짝은 `android:app` 의 `platform/di/PlatformModule` 이다. */
internal val iosPortModule: Module = module {
    single<AudioPreviewPlayer> { IosAudioPreviewPlayerStub() }
    single<StepMeasurementController> { IosStepMeasurementController(get(), get(), get(), get()) }
    single<FileShareServerController> { IosFileShareServerController(get(), get(), get()) }
    single<ReceiveKeepAlive> { IosReceiveKeepAlive() }
    single<ExecuteYoutubeDownload> { IosExecuteYoutubeDownload(get(), get(), get(), get()) }
    single<ExecuteAnomalyDataCheck> { IosExecuteAnomalyDataCheck(get(), get()) }
    single<ExecuteLyricsVariantCreation> { IosExecuteLyricsVariantCreation(get(), get()) }
    single<ExecuteAudioEditSave> { IosExecuteAudioEditSaveStub() }
    single<DataMigrationSendController> { IosDataMigrationSendControllerStub() }
    single<RunningWorkMonitor> { IosRunningWorkMonitor(get(), get()) }
}
