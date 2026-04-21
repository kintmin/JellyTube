package com.kintmin.jellytube

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.kintmin.presentation.theme.JellyTubeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.registerUser()
        viewModel.initializeMediaController()
        viewModel.handleIntent(intent)
        installSplashScreen()

        enableEdgeToEdge()
        setTheme(R.style.Theme_JellyTube)

        setContent {
            val navController = rememberNavController()
            JellyTubeTheme {
                Surface {
                    MainNavHost(
                        navController = navController,
                        deepLinkFlow = viewModel.deepLinkFlow,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIntent(intent)
    }

    override fun onDestroy() {
        viewModel.releaseMediaController()
        super.onDestroy()
    }
}
