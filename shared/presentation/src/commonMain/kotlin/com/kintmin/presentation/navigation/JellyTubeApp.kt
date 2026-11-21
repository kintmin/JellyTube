package com.kintmin.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.rememberNavController
import com.kintmin.presentation.animation.AppEntrance
import com.kintmin.presentation.platform.KeepScreenOnEffect
import com.kintmin.presentation.platform.PlatformToastHost
import com.kintmin.presentation.platform.keepsReceivingInBackground
import com.kintmin.presentation.port.FileShareServerController
import com.kintmin.presentation.theme.JellyTubeTheme
import kotlinx.coroutines.flow.Flow
import org.koin.compose.koinInject

@Composable
fun JellyTubeApp(
    navigationEventFlow: Flow<NavigationEvent>,
    isDebugTabVisible: Boolean,
    onDeepLink: (url: String) -> Unit,
    onAppBackground: () -> Unit,
) {
    val navController = rememberNavController()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { onAppBackground() }

    // 앱을 나가면 받기가 멈추는 플랫폼은, 서버가 켜진 동안 어느 화면에서든 자동 잠금으로 서스펜드되지 않게 한다.
    if (!keepsReceivingInBackground) {
        val isFileShareRunning by koinInject<FileShareServerController>().isRunning.collectAsState()
        KeepScreenOnEffect(enabled = isFileShareRunning)
    }

    JellyTubeTheme {
        // 스플래시·런치 화면과 같은 색이라야 진입 페이드 중에 다른 면이 비치지 않는다.
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppEntrance {
                    MainNavHost(
                        navController = navController,
                        navigationEventFlow = navigationEventFlow,
                        isDebugTabVisible = isDebugTabVisible,
                        onDeepLink = onDeepLink,
                    )
                }
                // 화면 위에 떠야 하므로 NavHost 뒤에 둔다. Android 에서는 no-op 이다.
                PlatformToastHost()
            }
        }
    }
}
