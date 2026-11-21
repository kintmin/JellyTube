package com.kintmin.jellytube

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kintmin.jellytube.service.StepForegroundService
import com.kintmin.presentation.navigation.AppIntent
import com.kintmin.presentation.navigation.AppViewModel
import com.kintmin.presentation.navigation.JellyTubeApp
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.sendIntent(AppIntent.InitializeMediaController)
        handleIntent(intent)
        installSplashScreen()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        setTheme(R.style.Theme_JellyTube)

        setContent {
            JellyTubeApp(
                navigationEventFlow = viewModel.eventFlow,
                isDebugTabVisible = BuildConfig.DEBUG,
                onDeepLink = { url -> viewModel.sendIntent(AppIntent.HandleDeepLink(url)) },
                onAppBackground = { viewModel.sendIntent(AppIntent.OnAppBackground) },
            )
        }

        StepForegroundService.startService(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return

        val uri = intent.data
        if (uri != null) {
            intent.data = null  // 사용된 딥링크는 소비
            viewModel.sendIntent(AppIntent.HandleDeepLink(uri.toString()))
            return
        }

        val sharedUris = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.extraStream())
            Intent.ACTION_SEND_MULTIPLE -> intent.extraStreamList()
            else -> emptyList()
        }
        if (sharedUris.isNotEmpty()) {
            intent.removeExtra(Intent.EXTRA_STREAM)  // 재처리 방지
            viewModel.sendIntent(AppIntent.ImportSharedAudio(sharedUris.map { it.toString() }))
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.extraStream(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM)
        }

    @Suppress("DEPRECATION")
    private fun Intent.extraStreamList(): List<Uri> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }.orEmpty()
}
