package com.kintmin.ios.adapter

import com.kintmin.domain.file_share.ReceiveKeepAlive
import com.kintmin.ios.bridge.IosAppLogBridge
import com.kintmin.ios.bridge.IosMediaControllerBridge
import com.kintmin.ios.bridge.IosMigrationSourceBrowserBridge
import com.kintmin.log.AppLog
import com.kintmin.presentation.port.AudioPreviewPlayer
import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.StartAnomalyDataCheck
import com.kintmin.presentation.port.StartAudioEditSave
import com.kintmin.presentation.port.StartLyricsVariantCreation
import com.kintmin.presentation.port.StartYoutubeDownload
import com.kintmin.presentation.port.FileShareServerController
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.port.MigrationSourceDiscovery
import com.kintmin.presentation.port.RunningTaskMonitor
import com.kintmin.presentation.port.StepMeasurementController
import org.koin.core.module.Module
import org.koin.dsl.module

/** Swift 가 구현한 브릿지를 생성자에 넘겨야 해서 `val` 이 아니라 함수다. */
internal fun iosAdapterModule(
    mediaControllerBridge: IosMediaControllerBridge,
    appLogBridge: IosAppLogBridge,
    migrationSourceBrowserBridge: IosMigrationSourceBrowserBridge,
): Module = module {
    single<AppLog> { IosAppLog(appLogBridge, get()) }
    single<PlaybackController> { IosPlaybackController(mediaControllerBridge) }
    single<MigrationSourceDiscovery> { IosMigrationSourceDiscovery(migrationSourceBrowserBridge) }
    single<AudioPreviewPlayer> { IosAudioPreviewPlayerStub() }
    single<StepMeasurementController> { IosStepMeasurementController(get(), get(), get(), get()) }
    single<FileShareServerController> { IosFileShareServerController(get(), get(), get()) }
    single<ReceiveKeepAlive> { IosReceiveKeepAlive() }
    single<StartYoutubeDownload> { IosStartYoutubeDownload(get(), get(), get(), get()) }
    single<StartAnomalyDataCheck> { IosStartAnomalyDataCheck(get(), get()) }
    single<StartLyricsVariantCreation> { IosStartLyricsVariantCreation(get(), get()) }
    single<StartAudioEditSave> { IosStartAudioEditSaveStub() }
    single<DataMigrationSendController> { IosDataMigrationSendControllerStub() }
    single<RunningTaskMonitor> { IosRunningTaskMonitor(get(), get()) }
}
