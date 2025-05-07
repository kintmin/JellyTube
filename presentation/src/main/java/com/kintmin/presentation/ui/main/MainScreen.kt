package com.kintmin.presentation.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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

@Composable
fun MainScreen(
    initTabItem: MainTabItem,
    navigateToPlaylistDetail: (id: Int) -> Unit,
    navigateToPlaylistEdit: (id: Int) -> Unit,
    navigateToPlaylistAdd: (id: Int) -> Unit,
) {
    val context = LocalContext.current
    val downloadViewModel = hiltViewModel<YoutubeDownloadViewModel>()
    val playlistViewModel = hiltViewModel<PlaylistViewModel>()

    var selectedTab by remember { mutableStateOf(initTabItem) }
    val playlist by playlistViewModel.playlistFlow.collectAsState(initial = emptyList())

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        playlistViewModel.eventFlow.collect { event ->
            when (event) {
                is PlaylistEvent.NavigateToPlaylistDetailScreen -> navigateToPlaylistDetail(event.playlistInfo.id)
                is PlaylistEvent.NavigateToPlaylistEditScreen -> navigateToPlaylistEdit(event.playlistId)
                PlaylistEvent.NavigateToMediaSearchScreen -> selectedTab = MainTabItem.Search
                is PlaylistEvent.NavigateToPlaylistAddScreen -> navigateToPlaylistAdd(event.playlistId)
            }
        }
    }

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        LaunchedEffect(Unit) {
            if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        LaunchedEffect(Unit) {
            if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    MainScreen(
        selectedTab = selectedTab,
        onChangeSelectedTab = { newTab -> selectedTab = newTab },
        playlist = playlist,
        sendYoutubeDownloadIntent = downloadViewModel::sendIntent,
        sendPlaylistIntent = playlistViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    selectedTab: MainTabItem,
    onChangeSelectedTab: (MainTabItem) -> Unit,
    playlist: List<PlaylistItemUiState>,
    sendYoutubeDownloadIntent: (YoutubeDownloadIntent) -> Unit,
    sendPlaylistIntent: (PlaylistIntent) -> Unit,
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var currentUrl: String by remember { mutableStateOf("https://www.youtube.com/") }

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
                        onClick = { onChangeSelectedTab(tab) }
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
                },
                setWebView = { value -> webView = value },
                webView = webView,
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
            selectedTab = MainTabItem.Search,
            playlist = PlaylistItemUiState.getMockList(),
            sendYoutubeDownloadIntent = {},
            sendPlaylistIntent = {},
            onChangeSelectedTab = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPlayTabPreview() {
    JellyTubeTheme {
        MainScreen(
            selectedTab = MainTabItem.Playlist,
            playlist = PlaylistItemUiState.getMockList(),
            sendYoutubeDownloadIntent = {},
            sendPlaylistIntent = {},
            onChangeSelectedTab = {},
        )
    }
}