package com.kintmin.ios

import com.kintmin.data.di.dataCommonModule
import com.kintmin.data.di.dataIosModule
import com.kintmin.data.log.initSentry
import com.kintmin.data.python_bridge.IosPythonExecutorBridge
import com.kintmin.data.translation.IosLyricsTranslatorBridge
import com.kintmin.ios.bridge.IosMediaControllerBridge
import com.kintmin.ios.bridge.IosMigrationSourceBrowserBridge
import com.kintmin.ios.adapter.iosAdapterModule
import com.kintmin.presentation.di.presentationModule
import org.koin.core.context.startKoin
import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi

/**
 * Swift 가 `IosKoinKt.doInitIosKoin(...)` 으로 부른다.
 * 파일명이 Obj-C 클래스명을 정하므로 이 파일 이름을 바꾸면 Swift 호출부가 깨진다.
 */
@OptIn(ExperimentalNativeApi::class)
fun initIosKoin(
    pythonExecutorBridge: IosPythonExecutorBridge,
    lyricsTranslatorBridge: IosLyricsTranslatorBridge,
    mediaControllerBridge: IosMediaControllerBridge,
    migrationSourceBrowserBridge: IosMigrationSourceBrowserBridge,
) {
    val bundle = NSBundle.mainBundle
    val version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString")
    val build = bundle.objectForInfoDictionaryKey("CFBundleVersion")
    initSentry(
        isDebug = Platform.isDebugBinary,
        release = "${bundle.bundleIdentifier}@$version+$build",
    )
    startKoin {
        modules(
            dataCommonModule,
            dataIosModule(pythonExecutorBridge, lyricsTranslatorBridge),
            presentationModule,
            iosAdapterModule(mediaControllerBridge, migrationSourceBrowserBridge),
        )
    }
}
