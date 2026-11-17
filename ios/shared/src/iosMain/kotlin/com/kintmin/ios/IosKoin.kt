package com.kintmin.ios

import com.kintmin.data.di.dataCommonModule
import com.kintmin.data.di.dataIosModule
import com.kintmin.data.python_bridge.IosPythonExecutorBridge
import com.kintmin.data.translation.IosLyricsTranslatorBridge
import com.kintmin.ios.bridge.IosAppLogBridge
import com.kintmin.ios.bridge.IosMediaControllerBridge
import com.kintmin.ios.bridge.IosMigrationSourceBrowserBridge
import com.kintmin.ios.adapter.iosAdapterModule
import com.kintmin.presentation.di.presentationModule
import org.koin.core.context.startKoin

/**
 * Swift 가 `IosKoinKt.doInitIosKoin(...)` 으로 부른다.
 * 파일명이 Obj-C 클래스명을 정하므로 이 파일 이름을 바꾸면 Swift 호출부가 깨진다.
 *
 * `android:app` 의 `JellyTubeApplication` 과 짝이다. 한쪽에 모듈을 더하면 다른 쪽도 본다.
 */
fun initIosKoin(
    pythonExecutorBridge: IosPythonExecutorBridge,
    lyricsTranslatorBridge: IosLyricsTranslatorBridge,
    mediaControllerBridge: IosMediaControllerBridge,
    appLogBridge: IosAppLogBridge,
    migrationSourceBrowserBridge: IosMigrationSourceBrowserBridge,
) {
    startKoin {
        modules(
            dataCommonModule,
            dataIosModule(pythonExecutorBridge, lyricsTranslatorBridge),
            presentationModule,
            iosAdapterModule(mediaControllerBridge, appLogBridge, migrationSourceBrowserBridge),
        )
    }
}
