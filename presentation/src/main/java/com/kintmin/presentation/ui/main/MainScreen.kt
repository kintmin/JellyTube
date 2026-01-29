package com.kintmin.presentation.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.custom_ui.AgeColumn
import com.kintmin.presentation.ui.custom_ui.DataTableView
import com.kintmin.presentation.ui.custom_ui.DepartmentColumn
import com.kintmin.presentation.ui.custom_ui.LineColumn
import com.kintmin.presentation.ui.custom_ui.NameColumn
import com.kintmin.presentation.ui.custom_ui.PaymentColumn
import com.kintmin.presentation.ui.custom_ui.TempData
import com.kintmin.presentation.ui.player_bar.PlayerBar
import com.kintmin.presentation.ui.main.playlist.PlaylistEvent
import com.kintmin.presentation.ui.main.playlist.PlaylistIntent
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState
import com.kintmin.presentation.ui.main.playlist.PlaylistView
import com.kintmin.presentation.ui.main.playlist.PlaylistViewModel
import com.kintmin.presentation.ui.main.youtube_search.YoutubeDownloadIntent
import com.kintmin.presentation.ui.main.youtube_search.YoutubeDownloadViewModel
import com.kintmin.presentation.ui.main.youtube_search.YoutubeWebView
import com.kintmin.presentation.ui.main.youtube_search.YoutubeWebViewEvent
import com.kintmin.presentation.ui.player_bar.PlayerBarIntent
import com.kintmin.presentation.ui.player_bar.PlayerBarUiState
import com.kintmin.presentation.ui.player_bar.PlayerBarViewModel

@Composable
fun MainScreen(
    navigateToPlaylistDetail: (id: Int) -> Unit,
    navigateToPlaylistEdit: (id: Int) -> Unit,
    navigateToPlaylistAdd: (id: Int) -> Unit,
) {
    val context = LocalContext.current
    val mainViewModel = hiltViewModel<MainViewModel>()
    val downloadViewModel = hiltViewModel<YoutubeDownloadViewModel>()
    val playlistViewModel = hiltViewModel<PlaylistViewModel>()
    val playerBarViewModel = hiltViewModel<PlayerBarViewModel>()

    val playlist by playlistViewModel.playlistFlow.collectAsState()
    val selectedTab by mainViewModel.tabItem.collectAsState()
    val currentUrl by downloadViewModel.currentUrl.collectAsState()
    val currentMediaItem by playerBarViewModel.currentMediaItem.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    )
    LaunchedEffect(Unit) {
        downloadViewModel.eventFlow.collect { event ->
            when (event) {
                is YoutubeWebViewEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        playlistViewModel.eventFlow.collect { event ->
            when (event) {
                is PlaylistEvent.NavigateToPlaylistDetailScreen -> navigateToPlaylistDetail(event.playlistInfo.id)
                is PlaylistEvent.NavigateToPlaylistEditScreen -> navigateToPlaylistEdit(event.playlistId)
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
        playlist = playlist,
        playerBar = currentMediaItem,
        currentUrl = currentUrl,
        sendMainIntent = mainViewModel::sendIntent,
        sendYoutubeDownloadIntent = downloadViewModel::sendIntent,
        sendPlaylistIntent = playlistViewModel::sendIntent,
        sendPlayerBarIntent = playerBarViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    selectedTab: MainTabItem,
    playlist: List<PlaylistItemUiState>,
    playerBar: PlayerBarUiState,
    currentUrl: String,
    sendMainIntent: (MainScreenIntent) -> Unit,
    sendYoutubeDownloadIntent: (YoutubeDownloadIntent) -> Unit,
    sendPlaylistIntent: (PlaylistIntent) -> Unit,
    sendPlayerBarIntent: (PlayerBarIntent) -> Unit,
) {
    var webView: WebView? by remember { mutableStateOf(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val title = when (selectedTab) {
                MainTabItem.Search -> "다운받을 유튜브 영상 검색하기"
                MainTabItem.Playlist -> "플레이리스트"
                MainTabItem.Debug -> "테스트뷰"
            }
            TopAppBar(
                title = { Text(title) },
            )
        },
        floatingActionButton = {
            if (selectedTab == MainTabItem.Search) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 48.dp),
                    onClick = {
                        sendYoutubeDownloadIntent(YoutubeDownloadIntent.OnClickDownload(currentUrl))
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "추가"
                    )
                }
            }
        },
        bottomBar = {
            Column {
                PlayerBar(
                    data = playerBar,
                    sendIntent = sendPlayerBarIntent,
                )
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        label = { Text("음원추가") },
                        selected = selectedTab == MainTabItem.Search,
                        onClick = { sendMainIntent(MainScreenIntent.ChangeTab(MainTabItem.Search)) },
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.VideoLibrary, contentDescription = null) },
                        label = { Text("플레이리스트") },
                        selected = selectedTab == MainTabItem.Playlist,
                        onClick = { sendMainIntent(MainScreenIntent.ChangeTab(MainTabItem.Playlist)) },
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.BugReport, contentDescription = null) },
                        label = { Text("테스트뷰") },
                        selected = selectedTab == MainTabItem.Debug,
                        onClick = { sendMainIntent(MainScreenIntent.ChangeTab(MainTabItem.Debug)) },
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
                setWebView = { value -> webView = value },
                webView = webView,
                sendIntent = sendYoutubeDownloadIntent,
            )

            MainTabItem.Playlist -> PlaylistView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                data = playlist,
                sendIntent = sendPlaylistIntent,
                sendMainIntent = sendMainIntent,
            )

            MainTabItem.Debug -> DataTableView(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                dataList = TempData.getMockList(20),
                keySelector = { data -> data.id },
                fixedHeaderList = listOf(NameColumn(), DepartmentColumn()),
                flexibleHeaderList = listOf(AgeColumn(), PaymentColumn(), LineColumn())
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
            playerBar = PlayerBarUiState.getMock(),
            currentUrl = "",
            sendMainIntent = {},
            sendYoutubeDownloadIntent = {},
            sendPlaylistIntent = {},
            sendPlayerBarIntent = {},
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
            playerBar = PlayerBarUiState.getMock(),
            currentUrl = "",
            sendMainIntent = {},
            sendYoutubeDownloadIntent = {},
            sendPlaylistIntent = {},
            sendPlayerBarIntent = {},
        )
    }
}