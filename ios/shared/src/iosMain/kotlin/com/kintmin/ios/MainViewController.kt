package com.kintmin.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.kintmin.domain.app_setting.usecase.FetchIsStepEnabledFlowUseCase
import com.kintmin.presentation.navigation.AppViewModel
import com.kintmin.presentation.navigation.JellyTubeApp
import com.kintmin.presentation.port.StepMeasurementController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

/**
 * iOS 는 단일 윈도우라 앱 레벨 ViewModel 을 파일 최상위에 **한 벌만** 둔다.
 * Compose 밖(`onOpenURL`)에서 들어오는 딥링크를 화면과 같은 인스턴스로 넘겨야 하기 때문이다.
 *
 * `by lazy` 인 이유: 이 프로퍼티가 [initIosKoin] 보다 먼저 초기화되면 Koin 이 아직 없다.
 * 실제로 쓰이는 시점(첫 화면 구성 / 첫 딥링크)에 만든다.
 */
private val appViewModel: AppViewModel by lazy { KoinPlatform.getKoin().get() }

private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

/**
 * Koin 초기화 직후 한 번 부른다. `android:app` 의 `MainActivity.onCreate` 앞부분과 짝이다.
 */
fun startApp() {
    appViewModel.registerUser()
    appViewModel.initializeMediaController()
    restoreStepMeasurementIfEnabled()
}

/**
 * 설정이 켜져 있으면 걸음수 측정을 다시 건다.
 *
 * Android 는 `MainActivity` 가 포그라운드 서비스를 띄우고 서비스가 같은 판단을 한다.
 * iOS 는 상시 실행이 없으므로 앱이 뜰 때마다 여기서 다시 걸고, 그 안에서 꺼져 있던 구간을
 * 소급 조회로 메운다.
 */
private fun restoreStepMeasurementIfEnabled() {
    appScope.launch {
        val koin = KoinPlatform.getKoin()
        if (koin.get<FetchIsStepEnabledFlowUseCase>()().first()) {
            koin.get<StepMeasurementController>().start()
        }
    }
}

/** Swift 가 `MainViewControllerKt.mainViewController()` 로 부른다. */
fun mainViewController(): UIViewController = ComposeUIViewController {
    JellyTubeApp(
        navigationIntentFlow = appViewModel.navigationIntentFlow,
        // 디버그 탭은 안드로이드 디버그 빌드 전용이다. iOS 에는 아직 빌드 변형이 없다.
        isDebugTabVisible = false,
        onDeepLink = appViewModel::handleDeepLink,
    )
}

/**
 * SwiftUI 의 `onOpenURL` 이 넘겨준다.
 *
 * 검증은 전부 [AppViewModel.handleDeepLink] 안에서 돈다 — Swift 에 판정을 내보내지 않는다.
 */
fun handleDeepLink(url: String) {
    appViewModel.handleDeepLink(url)
}
