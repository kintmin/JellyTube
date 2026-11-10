package com.kintmin.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kintmin.presentation.platform.PlatformToastHost
import com.kintmin.presentation.theme.JellyTubeTheme
import kotlinx.coroutines.flow.Flow

/**
 * 앱의 루트 컴포저블. 두 플랫폼이 같은 것을 띄운다.
 *
 * 창·시스템바·스플래시처럼 Compose 밖의 일은 플랫폼 진입점이 하고, 그 안쪽은 전부 여기다.
 */
@Composable
fun JellyTubeApp(
    navigationIntentFlow: Flow<NavigationIntent>,
    isDebugTabVisible: Boolean,
    onDeepLink: (url: String) -> Unit,
) {
    val navController = rememberNavController()

    JellyTubeTheme {
        Surface {
            Box(modifier = Modifier.fillMaxSize()) {
                MainNavHost(
                    navController = navController,
                    navigationIntentFlow = navigationIntentFlow,
                    isDebugTabVisible = isDebugTabVisible,
                    onDeepLink = onDeepLink,
                )
                // 화면 위에 떠야 하므로 NavHost 뒤에 둔다. Android 에서는 no-op 이다.
                PlatformToastHost()
            }
        }
    }
}
