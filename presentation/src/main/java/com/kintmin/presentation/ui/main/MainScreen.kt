package com.kintmin.presentation.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.main.playlist.PlaylistEvent
import com.kintmin.presentation.ui.main.playlist.PlaylistIntent
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState
import com.kintmin.presentation.ui.main.playlist.PlaylistView
import com.kintmin.presentation.ui.main.playlist.PlaylistViewModel
import com.kintmin.presentation.ui.main.youtube_search.YoutubeDownloadIntent
import com.kintmin.presentation.ui.main.youtube_search.YoutubeDownloadViewModel
import com.kintmin.presentation.ui.main.youtube_search.YoutubeWebView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun MainScreen(
    initTabItem: MainTabItem,
    navigateToPlaylistDetail: (id: Int) -> Unit,
) {
    val context = LocalContext.current
    val downloadViewModel = hiltViewModel<YoutubeDownloadViewModel>()
    val playlistViewModel = hiltViewModel<PlaylistViewModel>()

    LaunchedEffect(Unit) {
        playlistViewModel.eventFlow.collect { event ->
            when (event) {
                is PlaylistEvent.NavigateToPlaylistDetailScreen -> {
                    navigateToPlaylistDetail(event.playlistInfo.id)
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



    MainScreen(
        initTabItem = initTabItem,
//        audioPlayDataListFlow = audioPlayViewModel.audioList,
//        sendAudioPlayIntent = audioPlayViewModel::sendIntent,
        playlistFlow = playlistViewModel.playlistFlow,
        sendYoutubeDownloadIntent = downloadViewModel::sendIntent,
        sendPlaylistIntent = playlistViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initTabItem: MainTabItem,
//    audioPlayDataListFlow: Flow<List<AudioPlayUiState>>,
//    sendAudioPlayIntent: (AudioPlayIntent) -> Unit,
    playlistFlow: Flow<List<PlaylistItemUiState>>,
    sendYoutubeDownloadIntent: (YoutubeDownloadIntent) -> Unit,
    sendPlaylistIntent: (PlaylistIntent) -> Unit,
) {
    var currentUrl: String by remember { mutableStateOf("https://www.youtube.com/") }
    var selectedTab by remember { mutableStateOf(initTabItem) }

    val playlist by playlistFlow.collectAsState(initial = emptyList())

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val title = when (selectedTab) {
                MainTabItem.Search -> "다운받을 유튜브 영상 검색하기"
                MainTabItem.Playlist -> "플레이리스트"
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
                        imageVector = Icons.Rounded.Add,
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

            MainTabItem.Playlist -> PlaylistView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                data = playlist,
                sendIntent = sendPlaylistIntent,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenSearchTabPreview() {
    JellyTubeTheme {
        MainScreen(
            initTabItem = MainTabItem.Search,
            playlistFlow = flowOf(PlaylistItemUiState.getMockList()),
            sendYoutubeDownloadIntent = {},
            sendPlaylistIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPlayTabPreview() {
    JellyTubeTheme {
        MainScreen(
            initTabItem = MainTabItem.Playlist,
            playlistFlow = flowOf(PlaylistItemUiState.getMockList()),
            sendYoutubeDownloadIntent = {},
            sendPlaylistIntent = {},
        )
    }
}