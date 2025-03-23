package com.kintmin.presentation.ui

import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kintmin.presentation.notification.NotificationUtil
import com.kintmin.presentation.theme.YTMusicBoxTheme
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

        enableEdgeToEdge()
        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            val context = LocalContext.current
            val viewModel: YoutubeDownloadViewModel = hiltViewModel()
            var currentUrl: String by remember { mutableStateOf("https://www.youtube.com/") }

//            LaunchedEffect(Unit) {
//                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                    viewModel.eventFlow.collect { event ->
//                        when (event) {
//                            is YoutubeDownloadViewModel.Event.ShowLoadingNotification -> {
//
//                            }
//                        }
//                    }
//                }
//            }

            YTMusicBoxTheme {
                RequestNotificationPermission()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        // centerAlignedTopAppBarColors
                        TopAppBar(
                            title = { Text("다운받을 유튜브 영상 검색하기") },
                            colors = TopAppBarDefaults.topAppBarColors(),
                            actions = {
                                IconButton(onClick = {
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "재생"
                                    )
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                viewModel.startDownload(currentUrl)
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            //contentColor = Color.White
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "추가"
                            )
                        }
                    }
                ) { innerPadding ->
                    YoutubeWebView(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        url = currentUrl,
                        onChangeUrl = { newUrl ->
                            currentUrl = newUrl
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (!granted) {
                    Toast.makeText(context, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )

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
