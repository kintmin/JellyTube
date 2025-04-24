package com.kintmin.presentation.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.paging.PagingData
import com.kintmin.platform.service.PlaybackService
import com.kintmin.presentation.theme.YTMusicBoxTheme
import com.kintmin.presentation.ui.audio_play.AudioPlayEvent
import com.kintmin.presentation.ui.audio_play.AudioPlayIntent
import com.kintmin.presentation.ui.audio_play.AudioPlayView
import com.kintmin.presentation.ui.audio_play.AudioPlayViewModel
import com.kintmin.presentation.ui.audio_play.list_item.AudioPlayUiState
import com.kintmin.presentation.ui.youtube_search.YoutubeDownloadIntent
import com.kintmin.presentation.ui.youtube_search.YoutubeDownloadViewModel
import com.kintmin.presentation.ui.youtube_search.YoutubeWebView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun MainScreen(initTabItem: MainTabItem) {
    val context = LocalContext.current
    val audioPlayViewModel = hiltViewModel<AudioPlayViewModel>()
    val downloadViewModel = hiltViewModel<YoutubeDownloadViewModel>()

    LaunchedEffect(Unit) {
        audioPlayViewModel.eventFlow.collect { event ->
            when (event) {
                is AudioPlayEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is AudioPlayEvent.RegisterPlaylist -> {
                    context.startService(
                        Intent(context, PlaybackService::class.java).apply {
                            putParcelableArrayListExtra(PlaybackService.EXTRA_PLAYLIST, event.playlist)
                            putExtra(PlaybackService.EXTRA_PLAYLIST_INDEX, event.startIndex)
                        }
                    )
                }
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (!granted) {
                    Toast.makeText(context, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        LaunchedEffect(Unit) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

    controllerFuture.addListener({
        // Call controllerFuture.get() to retrieve the MediaController.
        // MediaController implements the Player interface, so it can be
        // attached to the PlayerView UI component.

        //playerView.setPlayer(controllerFuture.get())
    }, ContextCompat.getMainExecutor(LocalContext.current))

    MainScreen(
        initTabItem = initTabItem,
        lazyPagingItems = audioPlayViewModel.audioPagingFlow,
        sendAudioPlayIntent = audioPlayViewModel::sendIntent,
        sendYoutubeDownloadIntent = downloadViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initTabItem: MainTabItem,
    lazyPagingItems: Flow<PagingData<AudioPlayUiState>>,
    sendAudioPlayIntent: (AudioPlayIntent) -> Unit,
    sendYoutubeDownloadIntent: (YoutubeDownloadIntent) -> Unit,
) {
    var currentUrl: String by remember { mutableStateOf("https://www.youtube.com/") }
    var selectedTab by remember { mutableStateOf(initTabItem) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val title = when (selectedTab) {
                MainTabItem.Search -> "다운받을 유튜브 영상 검색하기"
                MainTabItem.Play -> "음원 재생하기"
            }
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
        floatingActionButton = {
            if (selectedTab == MainTabItem.Search) {
                FloatingActionButton(
                    onClick = {
                        sendYoutubeDownloadIntent(YoutubeDownloadIntent.OnClickDownload(currentUrl))
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
                        icon = { Icon(tab.getIcon(), contentDescription = tab.getLabel()) },
                        label = { Text(tab.getLabel()) },
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
                currentUrl = currentUrl,
                onChangeUrl = { newUrl ->
                    currentUrl = newUrl
                }
            )

            MainTabItem.Play -> AudioPlayView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                lazyPagingItems = lazyPagingItems,
                isBasePlaylist = true,
                sendIntent = sendAudioPlayIntent,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenSearchTabPreview() {
    YTMusicBoxTheme {
        MainScreen(
            initTabItem = MainTabItem.Search,
            lazyPagingItems = flowOf(PagingData.from(AudioPlayUiState.getMockList())),
            sendAudioPlayIntent = {},
            sendYoutubeDownloadIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPlayTabPreview() {
    YTMusicBoxTheme {
        MainScreen(
            initTabItem = MainTabItem.Play,
            lazyPagingItems = flowOf(PagingData.from(AudioPlayUiState.getMockList())),
            sendAudioPlayIntent = {},
            sendYoutubeDownloadIntent = {},
        )
    }
}