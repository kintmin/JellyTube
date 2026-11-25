package com.kintmin.presentation.ui.playlist_detail

import androidx.compose.animation.core.EaseIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.animation.FocusSpreadOverlay
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.remember_state.rememberFocusSpreadState
import com.kintmin.presentation.remember_state.rememberReorderState
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.topAppBarDetailTitleFontSize
import com.kintmin.presentation.theme.topAppBarDetailTitleLineHeight
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.ui.common.BackNavigationIconButton
import com.kintmin.presentation.ui.common.CollapsibleSearchField
import com.kintmin.presentation.ui.common.ReorderableItem
import com.kintmin.presentation.ui.common.SearchToggleButton
import com.kintmin.presentation.ui.player_bar.PlayerBar
import com.kintmin.presentation.ui.player_bar.PlayerBarIntent
import com.kintmin.presentation.ui.player_bar.PlayerBarUiState
import com.kintmin.presentation.ui.player_bar.PlayerBarViewModel
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderEvent
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderIntent
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderUiState
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderView
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderViewModel
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListEmptyView
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListEvent
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListIntent
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListItemUiState
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListItemView
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListViewModel
import kotlin.math.min
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    navigateToBack: () -> Unit,
    navigateToAddAudioMediaScreen: (playlistId: Int, playlistName: String) -> Unit,
    navigateToPlaylistEditScreen: (playlistId: Int, focusAudioMediaId: Int?) -> Unit,
    navigateToAudioDetailScreen: (audioMediaId: Int) -> Unit,
    navigateToAudioEditScreen: (audioMediaId: Int) -> Unit,
    navigateToPlayerDetail: () -> Unit,
    focusAudioMediaId: Int? = null,
) {
    val headerViewModel = koinViewModel<PlaylistDetailHeaderViewModel>()
    val listViewModel = koinViewModel<PlaylistDetailListViewModel>()
    val playerBarViewModel = koinViewModel<PlayerBarViewModel>()

    val headerData by headerViewModel.uiState.collectAsState()
    val listUiState by listViewModel.uiState.collectAsState()
    val currentMediaItem by playerBarViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        headerViewModel.eventFlow.collect { event ->
            when (event) {
                PlaylistDetailHeaderEvent.NavigateToAddAudioMediaScreen -> navigateToAddAudioMediaScreen(playlistId, headerData.name)
                PlaylistDetailHeaderEvent.NavigateToEditPlaylistScreen -> navigateToPlaylistEditScreen(playlistId, null)
            }
        }
    }

    val toaster = rememberToaster()

    LaunchedEffect(Unit) {
        listViewModel.eventFlow.collect { event ->
            when (event) {
                is PlaylistDetailListEvent.NavigateToAudioDetailScreen -> navigateToAudioDetailScreen(event.audioMediaId)
                is PlaylistDetailListEvent.NavigateToAudioEditScreen -> navigateToAudioEditScreen(event.audioMediaId)
                is PlaylistDetailListEvent.NavigateToPlaylistEditScreen -> navigateToPlaylistEditScreen(
                    playlistId,
                    event.focusAudioMediaId,
                )
                is PlaylistDetailListEvent.ShowToast -> toaster.show(event.message)
            }
        }
    }

    PlaylistDetailScreen(
        playlistId = playlistId,
        navigateToBack = navigateToBack,
        headerData = headerData,
        audioPlayDataList = listUiState.audioList,
        isAudioListLoaded = listUiState.isLoaded,
        isBasePlaylist = headerData.isBasePlaylist,
        playerBar = currentMediaItem,
        navigateToPlayerDetail = navigateToPlayerDetail,
        focusAudioMediaId = focusAudioMediaId,
        sendPlaylistDetailListIntent = listViewModel::sendIntent,
        sendPlaylistDetailHeaderIntent = headerViewModel::sendIntent,
        sendPlayerBarIntent = playerBarViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    navigateToBack: () -> Unit,
    headerData: PlaylistDetailHeaderUiState,
    audioPlayDataList: List<PlaylistDetailListItemUiState>,
    isAudioListLoaded: Boolean,
    isBasePlaylist: Boolean,
    playerBar: PlayerBarUiState,
    navigateToPlayerDetail: () -> Unit,
    focusAudioMediaId: Int? = null,
    sendPlaylistDetailListIntent: (PlaylistDetailListIntent) -> Unit,
    sendPlaylistDetailHeaderIntent: (PlaylistDetailHeaderIntent) -> Unit,
    sendPlayerBarIntent: (PlayerBarIntent) -> Unit,
) {
    val scrollState = rememberLazyListState()
    val reorderState = rememberReorderState(
        listState = scrollState,
        dataList = audioPlayDataList,
        idOf = { it.id },
        // 목록이 상단 바 밑까지 그려진다.
        topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + topAppBarHeight,
    )
    var isSearchFieldVisible by rememberSaveable { mutableStateOf(false) }
    var searchInput by rememberSaveable { mutableStateOf("") }
    fun changeSearchInput(text: String) {
        searchInput = text
        sendPlaylistDetailListIntent(PlaylistDetailListIntent.OnChangeSearchText(text))
    }
    fun closeSearchField() {
        isSearchFieldVisible = false
        changeSearchInput("")
    }
    val focusSpread = rememberFocusSpreadState(
        listState = scrollState,
        focusId = focusAudioMediaId,
        ids = audioPlayDataList.map { it.id },
        indexOffset = 1,
    )
    val maxOffset = with(LocalDensity.current) { 280.dp.toPx() }
    val scrollOffset = if (scrollState.firstVisibleItemIndex == 0) {
        min(scrollState.firstVisibleItemScrollOffset.toFloat(), maxOffset)
    } else {
        maxOffset
    }
    val progress = EaseIn.transform(min(scrollOffset / maxOffset, 1f))
    val backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = progress)
    val isSearching = searchInput.isNotEmpty()
    // 재생 위치까지 든 playerBar 를 행 람다에서 읽으면 폴링마다 보이는 행이 전부 다시 그려진다.
    val nowPlayingAudioMediaId = playerBar.nowPlayingMediaIdIn(playlistId)
    val isPlaybackPlaying = playerBar.isPlaying

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                expandedHeight = topAppBarHeight,
                title = {
                    Text(
                        headerData.name,
                        fontSize = topAppBarDetailTitleFontSize,
                        lineHeight = topAppBarDetailTitleLineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = progress)
                    )
                },
                navigationIcon = {
                    BackNavigationIconButton(onClick = { navigateToBack() })
                },
                actions = {
                    SearchToggleButton(
                        isSearchFieldVisible = isSearchFieldVisible,
                        contentDescription = "음원 검색",
                        onClick = {
                            if (isSearchFieldVisible) closeSearchField() else isSearchFieldVisible = true
                        },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        bottomBar = {
            Column {
                PlayerBar(
                    data = playerBar,
                    sendIntent = sendPlayerBarIntent,
                    onClickBar = navigateToPlayerDetail,
                )
                Box(
                    modifier = Modifier
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                state = scrollState,
            ) {
                item {
                    PlaylistDetailHeaderView(
                        innerPaddingValues = innerPadding,
                        headerData = headerData,
                        isBasePlaylist = isBasePlaylist,
                        sendIntent = sendPlaylistDetailHeaderIntent,
                    )
                }
                if (isAudioListLoaded && audioPlayDataList.isEmpty()) {
                    item {
                        PlaylistDetailListEmptyView(
                            modifier = Modifier,
                            searchText = searchInput,
                            isBasePlaylist = isBasePlaylist,
                            onClickAdd = {
                                sendPlaylistDetailHeaderIntent(PlaylistDetailHeaderIntent.OnClickAdd)
                            },
                        )
                    }
                }

                // 끄는 동안의 순서는 reorderState 가 들고 있다. 검색 중에는 목록이 필터
                // 결과라 끌어 옮기는 뜻이 정의되지 않으므로 원본을 그대로 그린다.
                val rowList = if (isSearching) audioPlayDataList else reorderState.items
                itemsIndexed(
                    items = rowList,
                    key = { _, item -> item.id }
                ) { _, item ->
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .animateItem()
                        ) {
                            PlaylistDetailListItemView(
                                data = item,
                                modifier = Modifier.matchParentSize(),
                                canRemoveTrack = headerData.canRemoveTrack,
                                isSecretPlaylist = headerData.isSecretPlaylist,
                                isNowPlaying = item.id == nowPlayingAudioMediaId,
                                isPlaying = isPlaybackPlaying,
                                isDragging = false,
                                sendIntent = sendPlaylistDetailListIntent,
                            )
                            if (focusSpread.highlightedId == item.id) {
                                FocusSpreadOverlay(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clipToBounds(),
                                    progress = focusSpread.progress,
                                )
                            }
                        }
                    } else {
                        ReorderableItem(
                            state = reorderState,
                            id = item.id,
                            modifier = Modifier.height(80.dp),
                            longPressToStart = true,
                            onDragEnd = {
                                if (!reorderState.isOrderChanged) return@ReorderableItem
                                reorderState.getDraggingItemIndex()?.let {
                                    sendPlaylistDetailListIntent(
                                        PlaylistDetailListIntent.OnReorderAudioMedia(
                                            reorderData = item,
                                            targetData = audioPlayDataList[it],
                                        ),
                                    )
                                }
                            },
                        ) { dragHandleModifier ->
                            PlaylistDetailListItemView(
                                data = item,
                                // 끌기를 행 전체가 받는다. 안쪽 탭 제스처는 자식이라 그대로 산다.
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(dragHandleModifier),
                                canRemoveTrack = headerData.canRemoveTrack,
                                isSecretPlaylist = headerData.isSecretPlaylist,
                                isNowPlaying = item.id == nowPlayingAudioMediaId,
                                isPlaying = isPlaybackPlaying,
                                isDragging = reorderState.draggingItemId == item.id,
                                sendIntent = sendPlaylistDetailListIntent,
                            )
                            if (focusSpread.highlightedId == item.id) {
                                FocusSpreadOverlay(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clipToBounds(),
                                    progress = focusSpread.progress,
                                )
                            }
                        }
                    }
                }
            }

            CollapsibleSearchField(
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                visible = isSearchFieldVisible,
                value = searchInput,
                placeholder = "음원 제목 검색",
                onValueChange = ::changeSearchInput,
                onClose = ::closeSearchField,
            )
        }
    }
}

@Preview
@Composable
fun PlaylistDetailScreenPreview() {
    JellyTubeTheme {
        PlaylistDetailScreen(
            playlistId = 1,
            navigateToBack = {},
            headerData = PlaylistDetailHeaderUiState.getMock(),
            audioPlayDataList = PlaylistDetailListItemUiState.getMockList(),
            isAudioListLoaded = true,
            isBasePlaylist = false,
            playerBar = PlayerBarUiState.getMock(),
            navigateToPlayerDetail = {},
            sendPlaylistDetailListIntent = {},
            sendPlaylistDetailHeaderIntent = {},
            sendPlayerBarIntent = {},
        )
    }
}

@Preview
@Composable
fun PlaylistDetailScreenEmptyPreview() {
    JellyTubeTheme {
        PlaylistDetailScreen(
            playlistId = 1,
            navigateToBack = {},
            headerData = PlaylistDetailHeaderUiState.getMock(),
            audioPlayDataList = emptyList(),
            isAudioListLoaded = true,
            isBasePlaylist = false,
            playerBar = PlayerBarUiState.getMock(),
            navigateToPlayerDetail = {},
            sendPlaylistDetailListIntent = {},
            sendPlaylistDetailHeaderIntent = {},
            sendPlayerBarIntent = {},
        )
    }
}

