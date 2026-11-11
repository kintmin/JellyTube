package com.kintmin.presentation.di

import com.kintmin.presentation.port.AudioPreviewPlayer
import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.ExecuteAnomalyDataCheck
import com.kintmin.presentation.port.ExecuteAudioEditSave
import com.kintmin.presentation.port.ExecuteLyricsVariantCreation
import com.kintmin.presentation.port.ExecuteYoutubeDownload
import com.kintmin.presentation.port.FileShareServerController
import com.kintmin.presentation.port.IosAudioPreviewPlayerStub
import com.kintmin.presentation.port.IosDataMigrationSendControllerStub
import com.kintmin.presentation.port.IosExecuteAnomalyDataCheck
import com.kintmin.presentation.port.IosExecuteAudioEditSaveStub
import com.kintmin.presentation.port.IosExecuteLyricsVariantCreation
import com.kintmin.presentation.port.IosExecuteYoutubeDownload
import com.kintmin.presentation.port.IosFileShareServerControllerStub
import com.kintmin.presentation.port.IosStepMeasurementController
import com.kintmin.presentation.port.StepMeasurementController
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * `port/` 의 iOS 구현 바인딩. Android 쪽 짝은 `android:app` 의 `platform/di/PlatformModule` 이다.
 *
 * iOS 구현이 인터페이스와 같은 모듈에 있는 것은 `Context` 류가 필요 없고 Kotlin/Native 가
 * UIKit·AVFoundation·CoreMotion 인터롭을 기본 제공해서다.
 *
 * `MediaControllerManager` 만 여기 없다 — 구현이 Swift 엔진에 기대고, Swift 가 보려면
 * 프레임워크에 export 되는 모듈에 있어야 해서 조립 모듈(`:ios:shared`)이 가진다.
 */
val presentationIosModule: Module = module {
    single<AudioPreviewPlayer> { IosAudioPreviewPlayerStub() }
    single<StepMeasurementController> { IosStepMeasurementController(get(), get(), get()) }
    single<FileShareServerController> { IosFileShareServerControllerStub() }
    single<ExecuteYoutubeDownload> { IosExecuteYoutubeDownload(get(), get(), get(), get()) }
    single<ExecuteAnomalyDataCheck> { IosExecuteAnomalyDataCheck(get(), get()) }
    single<ExecuteLyricsVariantCreation> { IosExecuteLyricsVariantCreation(get(), get()) }
    single<ExecuteAudioEditSave> { IosExecuteAudioEditSaveStub() }
    single<DataMigrationSendController> { IosDataMigrationSendControllerStub() }
}
