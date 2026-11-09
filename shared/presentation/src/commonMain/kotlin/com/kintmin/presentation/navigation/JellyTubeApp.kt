package com.kintmin.presentation.navigation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
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
            MainNavHost(
                navController = navController,
                navigationIntentFlow = navigationIntentFlow,
                isDebugTabVisible = isDebugTabVisible,
                onDeepLink = onDeepLink,
            )
        }
    }
}
