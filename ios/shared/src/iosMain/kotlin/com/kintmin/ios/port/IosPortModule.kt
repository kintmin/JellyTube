package com.kintmin.ios.port

import com.kintmin.domain.file_share.ReceiveKeepAlive
import com.kintmin.ios.bridge.IosAppLogBridge
import com.kintmin.ios.bridge.IosMediaControllerBridge
import com.kintmin.ios.bridge.IosMigrationSourceBrowserBridge
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
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS 포트 바인딩. `android:app` 의 `appModule` 과 같은 자리다.
 *
 * Swift 가 구현한 브릿지를 생성자에 넘겨야 해서 `val` 이 아니라 함수다 —
 * `data` 의 `dataIosModule(...)` 과 같은 모양이다. 바인딩을 호출부에 흘리지 않는다.
 */
internal fun iosPortModule(
    mediaControllerBridge: IosMediaControllerBridge,
    appLogBridge: IosAppLogBridge,
    migrationSourceBrowserBridge: IosMigrationSourceBrowserBridge,
): Module = module {
    single<AppLog> { IosAppLog(appLogBridge, get()) }
    single<MediaControllerManager> { IosMediaControllerManager(mediaControllerBridge) }
    single<MigrationSourceDiscovery> { IosMigrationSourceDiscovery(migrationSourceBrowserBridge) }
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
