package com.kintmin.ytmusicbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.kintmin.presentation.theme.YTMusicBoxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            //rememberNavController()
            YTMusicBoxTheme {
                Surface {
                    MainNavHost()
                }
            }
        }
    }
}
