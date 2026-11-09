package com.kintmin.presentation.di

import com.kintmin.presentation.port.AudioPreviewPlayer
import com.kintmin.presentation.port.ExecuteAnomalyDataCheck
import com.kintmin.presentation.port.ExecuteAudioEditSave
import com.kintmin.presentation.port.ExecuteLyricsVariantCreation
import com.kintmin.presentation.port.ExecuteYoutubeDownload
import com.kintmin.presentation.port.FileShareServerController
import com.kintmin.presentation.port.IosAudioPreviewPlayerStub
import com.kintmin.presentation.port.IosExecuteAnomalyDataCheckStub
import com.kintmin.presentation.port.IosExecuteAudioEditSaveStub
import com.kintmin.presentation.port.IosExecuteLyricsVariantCreationStub
import com.kintmin.presentation.port.IosExecuteYoutubeDownload
import com.kintmin.presentation.port.IosFileShareServerControllerStub
import com.kintmin.presentation.port.IosStepMeasurementController
import com.kintmin.presentation.port.StepMeasurementController
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * `port/` 의 iOS 구현 바인딩. Android 쪽 짝은 `android:app` 의 `platform/di/PlatformModule` 이다.
 *
 * `MediaControllerManager` 만 여기 없다 — 구현이 Swift 엔진에 기대고, Swift 가 보려면
 * 프레임워크에 export 되는 모듈에 있어야 해서 조립 모듈(`:ios:shared`)이 가진다.
 *
 * iOS 구현이 이 모듈 안에 있는 것은 `Context` 류가 필요 없고 Kotlin/Native 가
 * UIKit·AVFoundation·CoreMotion 인터롭을 기본 제공해서다. 인터페이스가 포트로 남아 있는 것은
 * Android 구현이 다른 모듈에 있기 때문이고, 그 사실은 바뀌지 않는다.
 */
val presentationIosModule: Module = module {
    single<AudioPreviewPlayer> { IosAudioPreviewPlayerStub() }
    single<StepMeasurementController> { IosStepMeasurementController(get(), get()) }
    single<FileShareServerController> { IosFileShareServerControllerStub() }
    single<ExecuteYoutubeDownload> { IosExecuteYoutubeDownload(get(), get(), get(), get()) }
    single<ExecuteAnomalyDataCheck> { IosExecuteAnomalyDataCheckStub() }
    single<ExecuteLyricsVariantCreation> { IosExecuteLyricsVariantCreationStub() }
    single<ExecuteAudioEditSave> { IosExecuteAudioEditSaveStub() }
}
