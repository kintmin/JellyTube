package com.kintmin.ios

import com.kintmin.data.di.dataCommonModule
import com.kintmin.data.di.dataIosModule
import com.kintmin.data.python_bridge.IosPythonExecutorBridge
import com.kintmin.domain.playlist.usecase.EnsureSystemPlaylistsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

private val iosAppScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private class IosSystemPlaylistInitializer : KoinComponent {
    fun ensure() {
        iosAppScope.launch { get<EnsureSystemPlaylistsUseCase>()() }
    }
}

/**
 * Swift 가 `IosKoinKt.doInitIosKoin(pythonExecutorBridge:)` 로 부른다.
 * 파일명이 Obj-C 클래스명을 정하므로 이 파일 이름을 바꾸면 Swift 호출부가 깨진다.
 */
fun initIosKoin(pythonExecutorBridge: IosPythonExecutorBridge) {
    startKoin {
        modules(
            dataCommonModule,
            dataIosModule(pythonExecutorBridge),
        )
    }

    // DB 콜백 시딩을 제거했으므로 시작 시 시스템 플레이리스트를 보장한다.
    IosSystemPlaylistInitializer().ensure()
}
