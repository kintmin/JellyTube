package com.kintmin.ytmusicbox

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.ytmusicbox.service.MediaPlayerService
import com.kintmin.ytmusicbox.ui.theme.YTMusicBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YTMusicBoxTheme {
                RequestNotificationPermission()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MusicControls(
                        modifier = Modifier.padding(innerPadding),
                        onPlayClick = { startMediaPlayerService() },
                        onStopClick = { stopMediaPlayerService() }
                    )
                }
            }
        }
    }

    private fun startMediaPlayerService() {
        val intent = Intent(this, MediaPlayerService::class.java)
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
fun MusicControls(modifier: Modifier = Modifier, onPlayClick: () -> Unit, onStopClick: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onPlayClick, modifier = Modifier.padding(8.dp)) {
            Text("음악 재생하기")
        }
        Button(onClick = onStopClick, modifier = Modifier.padding(8.dp)) {
            Text("음악 종료하기")
        }
    }
}

@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current
    LocalActivityResultRegistryOwner.current

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
