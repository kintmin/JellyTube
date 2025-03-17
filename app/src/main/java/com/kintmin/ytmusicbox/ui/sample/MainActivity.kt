package com.kintmin.ytmusicbox.ui.sample

import android.content.Intent
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
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.ytmusicbox.service.MediaPlayerService
import com.kintmin.ytmusicbox.ui.theme.YTMusicBoxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: YoutubeDownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YTMusicBoxTheme {
                RequestNotificationPermission()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MusicControls(
                        modifier = Modifier.padding(innerPadding),
                        onPlayClick = { id -> startMediaPlayerService(id)},
                        onStopClick = { stopMediaPlayerService() }
                    )
                }
            }
        }
    }

    private fun startMediaPlayerService(id: String) {
        val intent = Intent(this, MediaPlayerService::class.java).apply {
            putExtra(MediaPlayerService.EXTRA_AUDIO_ID, id)
        }
        startForegroundService(intent)
        Toast.makeText(this, "음악 재생 시작", Toast.LENGTH_SHORT).show()
    }

    private fun stopMediaPlayerService() {
        val intent = Intent(this, MediaPlayerService::class.java)
        stopService(intent)
        Toast.makeText(this, "음악 종료", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MusicControls(
    viewModel: YoutubeDownloadViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onPlayClick: (String) -> Unit,
    onStopClick: () -> Unit
) {
    val context = LocalContext.current
    val state = viewModel.state.collectAsState()

    when (val currentState = state.value) {
        YoutubeDownloadConstants.State.Idle -> viewModel.startDownload("https://www.youtube.com/watch?v=su5D_3gBHzU")
        YoutubeDownloadConstants.State.Loading -> Toast.makeText(context, "로딩중", Toast.LENGTH_SHORT).show()
        is YoutubeDownloadConstants.State.Success -> onPlayClick(currentState.id)
        is YoutubeDownloadConstants.State.Failed -> Toast.makeText(context, currentState.errorMessage, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onStopClick, modifier = Modifier.padding(8.dp)) {
            Text("음악 종료하기")
        }
        Button(onClick = {
            viewModel.startDownload("https://www.youtube.com/watch?v=su5D_3gBHzU")
        }, modifier = Modifier.padding(8.dp)) {
            Text("음악 재시작")
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
        MusicControls(
            onPlayClick = {},
            onStopClick = {}
        )
    }
}
