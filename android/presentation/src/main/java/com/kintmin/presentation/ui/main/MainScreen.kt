package com.kintmin.presentation.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.kintmin.presentation.BuildConfig
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.custom_ui.data_table.AgeColumn
import com.kintmin.presentation.ui.custom_ui.data_table.DataTableView
import com.kintmin.presentation.ui.custom_ui.data_table.DepartmentColumn
import com.kintmin.presentation.ui.custom_ui.data_table.LineColumn
import com.kintmin.presentation.ui.custom_ui.data_table.NameColumn
import com.kintmin.presentation.ui.custom_ui.data_table.PaymentColumn
import com.kintmin.presentation.ui.custom_ui.data_table.TempData
import com.kintmin.presentation.ui.player_bar.PlayerBar
import com.kintmin.presentation.ui.main.floating_action.MainFloatingActionButton
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
    navigateToSetting: () -> Unit,
    navigateToPlayerDetail: () -> Unit,
    navigateToFileShareReceive: () -> Unit,
) {
    val context = LocalContext.current
    val mainViewModel = koinViewModel<MainViewModel>()
    val downloadViewModel = koinViewModel<YoutubeDownloadViewModel>()
    val playlistViewModel = koinViewModel<PlaylistViewModel>()
    val playerBarViewModel = koinViewModel<PlayerBarViewModel>()

    val playlist by playlistViewModel.playlistFlow.collectAsState()
    val selectedTab by mainViewModel.tabItem.collectAsState()
    val currentUrl by downloadViewModel.currentUrl.collectAsState()
    val currentMediaItem by playerBarViewModel.currentMediaItem.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { _ -> },
    )
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        mainViewModel.sendIntent(MainScreenIntent.ImportMediaFiles(uris.map { it.toString() }))
    }
    LaunchedEffect(Unit) {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        downloadViewModel.eventFlow.collect { event ->
            when (event) {
                is YoutubeWebViewEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.eventFlow.collect { event ->
            when (event) {
                is MainScreenEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
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

    MainScreen(
        selectedTab = selectedTab,
        playlist = playlist,
        playerBar = currentMediaItem,
        currentUrl = currentUrl,
        navigateToSetting = navigateToSetting,
        navigateToPlayerDetail = navigateToPlayerDetail,
        navigateToFileShareReceive = navigateToFileShareReceive,
        onClickPickMedia = { fileLauncher.launch(arrayOf("audio/*")) },
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
    navigateToSetting: () -> Unit,
    navigateToPlayerDetail: () -> Unit,
    navigateToFileShareReceive: () -> Unit,
    onClickPickMedia: () -> Unit,
    sendMainIntent: (MainScreenIntent) -> Unit,
    sendYoutubeDownloadIntent: (YoutubeDownloadIntent) -> Unit,
    sendPlaylistIntent: (PlaylistIntent) -> Unit,
    sendPlayerBarIntent: (PlayerBarIntent) -> Unit,
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var isSearchFieldVisible by rememberSaveable { mutableStateOf(false) }
    var searchInput by rememberSaveable { mutableStateOf(currentUrl) }
    val tabSaveableStateHolder = rememberSaveableStateHolder()
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submitSearchUrl() {
        val normalizedUrl = normalizeUrl(searchInput)
        if (normalizedUrl.isBlank()) return
        searchInput = normalizedUrl
        sendYoutubeDownloadIntent(YoutubeDownloadIntent.OnChangeUrl(normalizedUrl))
        keyboardController?.hide()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val title = when (selectedTab) {
                MainTabItem.Search -> "다운받을 유튜브 영상 검색하기"
                MainTabItem.Playlist -> "플레이리스트"
                MainTabItem.Debug -> "테스트뷰"
            }
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            modifier = if (selectedTab == MainTabItem.Search) {
                                Modifier.clickable {
                                    sendYoutubeDownloadIntent(
                                        YoutubeDownloadIntent.OnChangeUrl("https://m.youtube.com/")
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                    },
                    actions = {
                        if (selectedTab == MainTabItem.Search) {
                            IconButton(
                                onClick = {
                                    val showSearchBar = !isSearchFieldVisible
                                    isSearchFieldVisible = showSearchBar
                                    if (showSearchBar) {
                                        searchInput = currentUrl
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = "검색창 열기",
                                )
                            }
                        }
                        if (selectedTab == MainTabItem.Playlist) {
                            IconButton(
                                onClick = navigateToSetting,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "설정 화면으로 이동",
                                )
                            }
                        }
                    },
                )

                AnimatedVisibility(
                    visible = selectedTab == MainTabItem.Search && isSearchFieldVisible,
                    enter = slideInVertically(initialOffsetY = { fullHeight -> -fullHeight }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { fullHeight -> -fullHeight }) + fadeOut(),
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        label = { Text("Youtube URL 붙여넣기") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submitSearchUrl() }),
                        trailingIcon = {
                            IconButton(onClick = { submitSearchUrl() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "URL 로드",
                                )
                            }
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == MainTabItem.Search) {
                MainFloatingActionButton(
                    currentUrl = currentUrl,
                    onClickDownload = { url ->
                        sendYoutubeDownloadIntent(YoutubeDownloadIntent.OnClickDownload(url))
                    },
                    onClickPickMedia = onClickPickMedia,
                    onClickDesktopFileShare = navigateToFileShareReceive,
                )
            }
        },
        bottomBar = {
            Column {
                PlayerBar(
                    data = playerBar,
                    sendIntent = sendPlayerBarIntent,
                    onClickBar = navigateToPlayerDetail,
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
                    if (BuildConfig.DEBUG) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Rounded.BugReport, contentDescription = null) },
                            label = { Text("테스트뷰") },
                            selected = selectedTab == MainTabItem.Debug,
                            onClick = { sendMainIntent(MainScreenIntent.ChangeTab(MainTabItem.Debug)) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        tabSaveableStateHolder.SaveableStateProvider(key = selectedTab.name) {
            when (selectedTab) {
                MainTabItem.Search -> YoutubeWebView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    currentUrl = currentUrl,
                    setWebView = { value -> webView = value },
                    webView = webView,
                    sendIntent = sendYoutubeDownloadIntent,
                    onNavigateToPlaylist = {
                        sendMainIntent(MainScreenIntent.ChangeTab(MainTabItem.Playlist))
                    },
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    dataList = TempData.getMockList(350),
                    keySelector = { data -> data.id },
                    fixedHeaderList = listOf(NameColumn(), DepartmentColumn()),
                    flexibleHeaderList = listOf(AgeColumn(), PaymentColumn()) + List(30) { LineColumn() }
                )
            }
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
            navigateToSetting = {},
            navigateToPlayerDetail = {},
            navigateToFileShareReceive = {},
            onClickPickMedia = {},
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
            navigateToSetting = {},
            navigateToPlayerDetail = {},
            navigateToFileShareReceive = {},
            onClickPickMedia = {},
            sendMainIntent = {},
            sendYoutubeDownloadIntent = {},
            sendPlaylistIntent = {},
            sendPlayerBarIntent = {},
        )
    }
}

private fun normalizeUrl(rawInput: String): String {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank()) return ""

    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

