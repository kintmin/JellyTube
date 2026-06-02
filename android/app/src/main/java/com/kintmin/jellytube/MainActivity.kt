package com.kintmin.jellytube

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.kintmin.platform.service.StepForegroundService
import com.kintmin.presentation.theme.JellyTubeTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.registerUser()
        viewModel.initializeMediaController()
        viewModel.handleIntent(intent)
        installSplashScreen()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        setTheme(R.style.Theme_JellyTube)

        setContent {
            val navController = rememberNavController()
            JellyTubeTheme {
                Surface {
                    MainNavHost(
                        navController = navController,
                        navigationIntentFlow = viewModel.navigationIntentFlow,
                        onDeepLink = viewModel::onDeepLink,
                    )
                }
            }
        }

        StepForegroundService.startService(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleIntent(intent)
        setIntent(intent)
    }
}
