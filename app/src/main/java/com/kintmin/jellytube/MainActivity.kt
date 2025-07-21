package com.kintmin.jellytube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.kintmin.platform.util.MediaControllerManager
import com.kintmin.presentation.theme.JellyTubeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var mediaControllerManager: MediaControllerManager

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.registerUser()
        installSplashScreen()

        enableEdgeToEdge()
        setTheme(R.style.Theme_JellyTube)

        setContent {
            val navController = rememberNavController()
            JellyTubeTheme {
                Surface {
                    MainNavHost(navController = navController)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            mediaControllerManager.initialize(baseContext)
        }
    }

    override fun onDestroy() {
        runCatching {
            mediaControllerManager.release()
        }
        super.onDestroy()
    }
}
