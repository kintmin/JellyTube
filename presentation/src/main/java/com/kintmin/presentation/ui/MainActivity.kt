package com.kintmin.presentation.ui

import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.kintmin.presentation.notification.NotificationUtil
import com.kintmin.presentation.service.PlaybackService
import com.kintmin.presentation.theme.YTMusicBoxTheme
import com.kintmin.presentation.ui.audio_play.AudioPlayView
import com.kintmin.presentation.ui.youtube_search.YoutubeDownloadViewModel
import com.kintmin.presentation.ui.youtube_search.YoutubeWebView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificationUtil: NotificationUtil

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            // Call controllerFuture.get() to retrieve the MediaController.
            // MediaController implements the Player interface, so it can be
            // attached to the PlayerView UI component.

            //playerView.setPlayer(controllerFuture.get())
        }, ContextCompat.getMainExecutor(this))

        enableEdgeToEdge()
        setContent {
            val youtubeDownloadViewModel: YoutubeDownloadViewModel = hiltViewModel()
            var currentUrl: String by remember { mutableStateOf("https://www.youtube.com/") }
            var selectedTab by remember { mutableStateOf(MainTabItem.Play) }

//            val lifecycleOwner = LocalLifecycleOwner.current
//            val context = LocalContext.current
//            LaunchedEffect(Unit) {
//                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                    viewModel.eventFlow.collect { event ->

            YTMusicBoxTheme {
                RequestNotificationPermission(this)
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("다운받을 유튜브 영상 검색하기") },
                            colors = TopAppBarDefaults.topAppBarColors(),
                        )
                    },
                    floatingActionButton = {
                        if (selectedTab == MainTabItem.Search) {
                            FloatingActionButton(
                                onClick = {
                                    youtubeDownloadViewModel.startDownload(currentUrl)
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "추가"
                                )
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar {
                            MainTabItem.entries.forEach { tab ->
                                NavigationBarItem(
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        MainTabItem.Search -> YoutubeWebView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            url = currentUrl,
                            onChangeUrl = { newUrl ->
                                currentUrl = newUrl
                            }
                        )

                        MainTabItem.Play -> AudioPlayView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestNotificationPermission(activity: Activity) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MusicControlsPreview() {
    YTMusicBoxTheme {
    }
}
