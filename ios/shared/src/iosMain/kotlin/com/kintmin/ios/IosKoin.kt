package com.kintmin.ios

import com.kintmin.data.di.dataCommonModule
import com.kintmin.data.di.dataIosModule
import com.kintmin.data.python_bridge.IosPythonExecutorBridge
import com.kintmin.domain.step.worker.RegisterDailyResetImmediatelyWorker
import com.kintmin.domain.step.worker.RegisterLoadBalancedDailyResetWorker
import com.kintmin.ios.log.IosAppLog
import com.kintmin.ios.log.IosAppLogBridge
import com.kintmin.ios.media.IosMediaControllerBridge
import com.kintmin.ios.media.IosMediaControllerManager
import com.kintmin.ios.step.IosRegisterDailyResetImmediatelyWorker
import com.kintmin.ios.step.IosRegisterLoadBalancedDailyResetWorker
import com.kintmin.presentation.di.presentationIosModule
import com.kintmin.log.AppLog
import com.kintmin.presentation.port.MediaControllerManager
import com.kintmin.presentation.di.presentationModule
import org.koin.core.context.startKoin
import org.koin.dsl.module

/** 도메인 포트의 iOS 구현. 플랫폼 포트는 `presentationIosModule` 이 맡는다. */
private val iosDomainPortModule = module {
    single<RegisterDailyResetImmediatelyWorker> { IosRegisterDailyResetImmediatelyWorker() }
    single<RegisterLoadBalancedDailyResetWorker> { IosRegisterLoadBalancedDailyResetWorker() }
}

/**
 * Swift 가 `IosKoinKt.doInitIosKoin(pythonExecutorBridge:)` 로 부른다.
 * 파일명이 Obj-C 클래스명을 정하므로 이 파일 이름을 바꾸면 Swift 호출부가 깨진다.
 *
 * `android:app` 의 `JellyTubeApplication` 과 짝이다. 한쪽에 모듈을 더하면 다른 쪽도 본다.
 */
fun initIosKoin(
    pythonExecutorBridge: IosPythonExecutorBridge,
    mediaControllerBridge: IosMediaControllerBridge,
    appLogBridge: IosAppLogBridge,
) {
    startKoin {
        modules(
            dataCommonModule,
            dataIosModule(pythonExecutorBridge),
            presentationModule,
            presentationIosModule,
            iosDomainPortModule,
            module {
                single<MediaControllerManager> { IosMediaControllerManager(mediaControllerBridge) }
                single<AppLog> { IosAppLog(appLogBridge, get()) }
            },
        )
    }
}
