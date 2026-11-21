package com.kintmin.presentation.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.animation.FloatingFieldVisibility
import com.kintmin.presentation.platform.AppPermission
import com.kintmin.presentation.platform.rememberPermissionController
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.platform.rememberWebViewHandle
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.theme.topAppBarIconSize
import com.kintmin.presentation.theme.topAppBarRootTitleFontSize
import com.kintmin.presentation.theme.topAppBarRootTitleLineHeight
import com.kintmin.presentation.ui.common.floatingField
import com.kintmin.presentation.ui.custom_ui.data_table.AgeColumn
import com.kintmin.presentation.ui.custom_ui.data_table.DataTableView
import com.kintmin.presentation.ui.custom_ui.data_table.DepartmentColumn
import com.kintmin.presentation.ui.custom_ui.data_table.LineColumn
import com.kintmin.presentation.ui.custom_ui.data_table.NameColumn
import com.kintmin.presentation.ui.custom_ui.data_table.PaymentColumn
import com.kintmin.presentation.ui.custom_ui.data_table.TempData
import com.kintmin.presentation.ui.main.bottom_sheet.PlaylistReorderBottomSheet
import com.kintmin.presentation.ui.main.floating_action.MainFloatingActionButton
import com.kintmin.presentation.ui.main.playlist.PlaylistEvent
import com.kintmin.presentation.ui.main.playlist.PlaylistIntent
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState
import com.kintmin.presentation.ui.main.playlist.PlaylistView
import com.kintmin.presentation.ui.main.playlist.PlaylistViewModel
import com.kintmin.presentation.ui.main.youtube_search.YoutubeDownloadEvent
import com.kintmin.presentation.ui.main.youtube_search.YoutubeDownloadIntent
import com.kintmin.presentation.ui.main.youtube_search.YoutubeDownloadViewModel
import com.kintmin.presentation.ui.main.youtube_search.YoutubeUrlField
import com.kintmin.presentation.ui.main.youtube_search.YoutubeWebView
import com.kintmin.presentation.ui.player_bar.PlayerBar
import com.kintmin.presentation.ui.player_bar.PlayerBarIntent
import com.kintmin.presentation.ui.player_bar.PlayerBarUiState
import com.kintmin.presentation.ui.player_bar.PlayerBarViewModel
import org.koin.compose.viewmodel.koinViewModel

private val NAV_ICON_SIZE = 22.dp
private val NAV_LABEL_FONT_SIZE = 11.sp

@Composable
fun MainScreen(
    isDebugTabVisible: Boolean,
    hintSecretPlaylistEntry: Boolean,
    navigateToPlaylistDetail: (id: Int, isBasePlaylist: Boolean) -> Unit,
    navigateToPlaylistEdit: (id: Int) -> Unit,
    navigateToPlaylistAdd: (id: Int, playlistName: String) -> Unit,
    navigateToSetting: () -> Unit,
    navigateToSecretPlaylist: () -> Unit,
    navigateToPlayerDetail: () -> Unit,
    navigateToPlaylistDownload: (url: String) -> Unit,
) {
    val toaster = rememberToaster()
    val mainViewModel = koinViewModel<MainViewModel>()
    val downloadViewModel = koinViewModel<YoutubeDownloadViewModel>()
    val playlistViewModel = koinViewModel<PlaylistViewModel>()
    val playerBarViewModel = koinViewModel<PlayerBarViewModel>()

    val playlistUiState by playlistViewModel.uiState.collectAsState()
    val mainUiState by mainViewModel.uiState.collectAsState()
    val youtubeDownloadUiState by downloadViewModel.uiState.collectAsState()
    val currentMediaItem by playerBarViewModel.uiState.collectAsState()

    val permissionController = rememberPermissionController()
    LaunchedEffect(Unit) {
        // 어떤 권한이 OS 버전별로 실제 필요한지는 구현이 판단한다.
        permissionController.request(
            listOf(AppPermission.MediaStorage, AppPermission.Notification),
        )
    }

    LaunchedEffect(Unit) {
        downloadViewModel.eventFlow.collect { event ->
            when (event) {
                is YoutubeDownloadEvent.ShowToast -> toaster.show(event.message)
            }
        }
    }

    LaunchedEffect(Unit) {
        playlistViewModel.eventFlow.collect { event ->
            when (event) {
                is PlaylistEvent.NavigateToPlaylistDetailScreen -> navigateToPlaylistDetail(event.playlistInfo.id, event.playlistInfo.isBasePlaylist)
                is PlaylistEvent.NavigateToPlaylistEditScreen -> navigateToPlaylistEdit(event.playlistId)
                is PlaylistEvent.NavigateToPlaylistAddScreen -> navigateToPlaylistAdd(event.playlistId, event.playlistName)
                is PlaylistEvent.ShowToast -> toaster.show(event.message)
            }
        }
    }

    MainScreen(
        isDebugTabVisible = isDebugTabVisible,
        hintSecretPlaylistEntry = hintSecretPlaylistEntry,
        selectedTab = mainUiState.tabItem,
        playlist = playlistUiState.playlists,
        playerBar = currentMediaItem,
        currentUrl = youtubeDownloadUiState.currentUrl,
        navigateToSetting = navigateToSetting,
        navigateToSecretPlaylist = navigateToSecretPlaylist,
        navigateToPlayerDetail = navigateToPlayerDetail,
        navigateToPlaylistDownload = navigateToPlaylistDownload,
        sendMainIntent = mainViewModel::sendIntent,
        sendYoutubeDownloadIntent = downloadViewModel::sendIntent,
        sendPlaylistIntent = playlistViewModel::sendIntent,
        sendPlayerBarIntent = playerBarViewModel::sendIntent,
    )

    if (playlistUiState.isReorderBottomSheetVisible) {
        PlaylistReorderBottomSheet(
            playlists = playlistUiState.playlists.filterNot { it.isBasePlaylist },
            onReorder = { orderedIds ->
                playlistViewModel.sendIntent(PlaylistIntent.OnReorderPlaylist(orderedIds))
            },
            onDismissRequest = {
                playlistViewModel.sendIntent(PlaylistIntent.OnDismissReorderBottomSheet)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isDebugTabVisible: Boolean,
    selectedTab: MainTabItem,
    hintSecretPlaylistEntry: Boolean = false,
    playlist: List<PlaylistItemUiState>,
    playerBar: PlayerBarUiState,
    currentUrl: String,
    navigateToSetting: () -> Unit,
    navigateToSecretPlaylist: () -> Unit,
    navigateToPlayerDetail: () -> Unit,
    navigateToPlaylistDownload: (url: String) -> Unit,
    sendMainIntent: (MainIntent) -> Unit,
    sendYoutubeDownloadIntent: (YoutubeDownloadIntent) -> Unit,
    sendPlaylistIntent: (PlaylistIntent) -> Unit,
    sendPlayerBarIntent: (PlayerBarIntent) -> Unit,
) {
    // 탭을 옮겨도 인스턴스를 유지하려고 여기서 만든다.
    val webViewHandle = rememberWebViewHandle()

    var isSearchFieldVisible by rememberSaveable { mutableStateOf(false) }
    var searchInput by rememberSaveable { mutableStateOf("") }
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
                    expandedHeight = topAppBarHeight,
                    title = {
                        LongPressHintBox(
                            isHintRequested = hintSecretPlaylistEntry && selectedTab == MainTabItem.Playlist,
                        ) {
                            Text(
                                text = title,
                                fontSize = topAppBarRootTitleFontSize,
                                lineHeight = topAppBarRootTitleLineHeight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = when (selectedTab) {
                                    MainTabItem.Search -> Modifier.clickable(onClickLabel = "유튜브 홈으로 이동", role = Role.Button) {
                                        sendYoutubeDownloadIntent(
                                            YoutubeDownloadIntent.OnChangeUrl("https://m.youtube.com/")
                                        )
                                    }
                                    // 숨은 입구다. 눌림 표시도 접근성 동작도 두지 않아 있는지 드러나지 않게 한다.
                                    MainTabItem.Playlist -> Modifier.pointerInput(Unit) {
                                        detectTapGestures(onLongPress = { navigateToSecretPlaylist() })
                                    }
                                    MainTabItem.Debug -> Modifier
                                }
                            )
                        }
                    },
                    actions = {
                        if (selectedTab == MainTabItem.Search) {
                            IconButton(
                                onClick = {
                                    val showSearchBar = !isSearchFieldVisible
                                    isSearchFieldVisible = showSearchBar
                                    if (showSearchBar) {
                                        searchInput = ""
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    modifier = Modifier.size(topAppBarIconSize),
                                    contentDescription = if (isSearchFieldVisible) "URL 입력창 닫기" else "URL 입력창 열기",
                                )
                            }
                        }
                        if (selectedTab == MainTabItem.Playlist) {
                            // 순서 변경은 내 플레이리스트에만 걸리므로 그 섹션 머리말이 갖는다.
                            // 상단바에는 화면 전체에 걸리는 것만 남긴다.
                            IconButton(
                                onClick = navigateToSetting,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    modifier = Modifier.size(topAppBarIconSize),
                                    contentDescription = "설정 화면으로 이동",
                                )
                            }
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == MainTabItem.Search) {
                MainFloatingActionButton(
                    currentUrl = currentUrl,
                    onClickDownload = { url ->
                        sendYoutubeDownloadIntent(YoutubeDownloadIntent.OnClickDownload(url))
                    },
                    navigateToPlaylistDownload = navigateToPlaylistDownload,
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
                // NavigationBar(80dp)는 높이를 줄이면 아이템이 잘린다. 64dp 짜리는 별도 컴포넌트다.
                // 아이템은 아이콘을 위에서부터 고정 배치해 내용이 줄면 그만큼 아래가 빈다. 그래서 조금만 줄인다.
                ShortNavigationBar {
                    // 이 테마는 선택·비선택 아이콘 토큰이 모두 gray100 이라 기본값으로 두면 두 상태가
                    // 같은 색이 된다. 색을 가르고, 색 하나에만 기대지 않도록 굵기로 한 번 더 가른다.
                    val navigationBarItemColors = ShortNavigationBarItemDefaults.colors().copy(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = contentMuted,
                        unselectedTextColor = contentMuted,
                    )
                    ShortNavigationBarItem(
                        icon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(NAV_ICON_SIZE)) },
                        label = {
                            Text(
                                text = "음원추가",
                                fontSize = NAV_LABEL_FONT_SIZE,
                                fontWeight = if (selectedTab == MainTabItem.Search) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        selected = selectedTab == MainTabItem.Search,
                        onClick = { sendMainIntent(MainIntent.ChangeTab(MainTabItem.Search)) },
                        colors = navigationBarItemColors,
                    )
                    ShortNavigationBarItem(
                        icon = { Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(NAV_ICON_SIZE)) },
                        label = {
                            Text(
                                text = "플레이리스트",
                                fontSize = NAV_LABEL_FONT_SIZE,
                                fontWeight = if (selectedTab == MainTabItem.Playlist) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        selected = selectedTab == MainTabItem.Playlist,
                        onClick = { sendMainIntent(MainIntent.ChangeTab(MainTabItem.Playlist)) },
                        colors = navigationBarItemColors,
                    )
                    if (isDebugTabVisible) {
                        ShortNavigationBarItem(
                            icon = { Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(NAV_ICON_SIZE)) },
                            label = {
                                Text(
                                    text = "테스트뷰",
                                    fontSize = NAV_LABEL_FONT_SIZE,
                                    fontWeight = if (selectedTab == MainTabItem.Debug) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            selected = selectedTab == MainTabItem.Debug,
                            onClick = { sendMainIntent(MainIntent.ChangeTab(MainTabItem.Debug)) },
                            colors = navigationBarItemColors,
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        tabSaveableStateHolder.SaveableStateProvider(key = selectedTab.name) {
            when (selectedTab) {
                MainTabItem.Search -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    YoutubeWebView(
                        currentUrl = currentUrl,
                        webViewHandle = webViewHandle,
                        sendIntent = sendYoutubeDownloadIntent,
                        onNavigateToPlaylist = {
                            sendMainIntent(MainIntent.ChangeTab(MainTabItem.Playlist))
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    FloatingFieldVisibility(visible = isSearchFieldVisible) {
                        YoutubeUrlField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .floatingField(),
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            onSubmit = { submitSearchUrl() },
                        )
                    }
                }

                MainTabItem.Playlist -> PlaylistView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    data = playlist,
                    sendIntent = sendPlaylistIntent,
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

@Preview
@Composable
fun MainScreenSearchTabPreview() {
    JellyTubeTheme {
        MainScreen(
            isDebugTabVisible = true,
            selectedTab = MainTabItem.Search,
            playlist = PlaylistItemUiState.getMockList(),
            playerBar = PlayerBarUiState.getMock(),
            currentUrl = "",
            navigateToSetting = {},
            navigateToSecretPlaylist = {},
            navigateToPlayerDetail = {},
            navigateToPlaylistDownload = {},
            sendMainIntent = {},
            sendYoutubeDownloadIntent = {},
            sendPlaylistIntent = {},
            sendPlayerBarIntent = {},
        )
    }
}

@Preview
@Composable
fun MainScreenPlayTabPreview() {
    JellyTubeTheme {
        MainScreen(
            isDebugTabVisible = true,
            selectedTab = MainTabItem.Playlist,
            playlist = PlaylistItemUiState.getMockList(),
            playerBar = PlayerBarUiState.getMock(),
            currentUrl = "",
            navigateToSetting = {},
            navigateToSecretPlaylist = {},
            navigateToPlayerDetail = {},
            navigateToPlaylistDownload = {},
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

