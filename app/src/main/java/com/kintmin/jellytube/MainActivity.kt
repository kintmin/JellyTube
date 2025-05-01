package com.kintmin.jellytube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.kintmin.platform.util.MediaControllerManager
import com.kintmin.presentation.theme.JellyTubeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var mediaControllerManager: MediaControllerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaControllerManager.initialize(baseContext)

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            JellyTubeTheme {
                Surface {
                    MainNavHost(navController = navController)
                }
            }
        }
    }
}
