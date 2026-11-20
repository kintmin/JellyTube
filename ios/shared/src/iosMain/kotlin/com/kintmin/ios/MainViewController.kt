package com.kintmin.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.kintmin.data.python_bridge.IosPythonExecutorBridge
import com.kintmin.domain.app_setting.usecase.FetchIsStepEnabledFlowUseCase
import com.kintmin.domain.user.usecase.RegisterUserUseCase
import com.kintmin.ios.notification.localNotificationDeepLinkOf
import com.kintmin.presentation.navigation.AppIntent
import com.kintmin.presentation.navigation.AppViewModel
import com.kintmin.presentation.navigation.JellyTubeApp
import com.kintmin.presentation.port.StartYoutubeDownload
import com.kintmin.presentation.port.StepMeasurementController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

/** Compose 밖(`onOpenURL`)에서 온 이벤트를 화면과 같은 인스턴스로 넘겨야 해서 한 벌만 둔다. */
private val appViewModel: AppViewModel by lazy { KoinPlatform.getKoin().get() }

private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

/** Koin 초기화 직후 한 번 부른다. */
fun startApp() {
    appScope.launch { KoinPlatform.getKoin().get<RegisterUserUseCase>()() }
    appViewModel.sendIntent(AppIntent.InitializeMediaController)
    restoreStepMeasurementIfEnabled()
    resumeFinishedDownloads()
}

/** 앱이 내려가 있는 사이에 끝난 전송은 받아 줄 코루틴이 없다. 같은 주소를 다시 태워 등록까지 잇는다. */
private fun resumeFinishedDownloads() {
    appScope.launch {
        val koin = KoinPlatform.getKoin()
        val finishedUrls = koin.get<IosPythonExecutorBridge>().takeFinishedDownloadUrls()
        if (finishedUrls.isEmpty()) return@launch

        val execute = koin.get<StartYoutubeDownload>()
        finishedUrls.forEach { execute(it) }
    }
}

/** iOS 는 상시 실행이 없어 앱이 뜰 때마다 다시 건다. 꺼져 있던 구간은 컨트롤러가 메운다. */
private fun restoreStepMeasurementIfEnabled() {
    appScope.launch {
        val koin = KoinPlatform.getKoin()
        if (koin.get<FetchIsStepEnabledFlowUseCase>()().first()) {
            koin.get<StepMeasurementController>().start()
        }
    }
}

fun mainViewController(): UIViewController = ComposeUIViewController {
    JellyTubeApp(
        navigationIntentFlow = appViewModel.eventFlow,
        // iOS 에는 대응하는 빌드 변형이 없다.
        isDebugTabVisible = false,
        onDeepLink = { url -> appViewModel.sendIntent(AppIntent.HandleDeepLink(url)) },
    )
}

/** SwiftUI 의 `onOpenURL` 이 넘겨준다. */
fun handleDeepLink(url: String) {
    appViewModel.sendIntent(AppIntent.HandleDeepLink(url))
}

fun handleNotificationTap(userInfo: Map<Any?, *>) {
    val deepLink = localNotificationDeepLinkOf(userInfo) ?: return
    appViewModel.sendIntent(AppIntent.HandleDeepLink(deepLink))
}

/** 다른 앱이 "JellyTube 로 열기"로 넘긴 `file://` 주소. */
fun handleSharedAudioFile(url: String) {
    appViewModel.sendIntent(AppIntent.ImportSharedAudio(listOf(url)))
}
