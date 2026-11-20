package com.kintmin.presentation.ui.playlist_detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.topAppBarDetailTitleFontSize
import com.kintmin.presentation.theme.topAppBarDetailTitleLineHeight
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.theme.topAppBarIconSize
import com.kintmin.presentation.animation.FocusSpreadOverlay
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.remember_state.rememberReorderState
import com.kintmin.presentation.ui.common.ReorderableItem
import com.kintmin.presentation.ui.player_bar.PlayerBar
import com.kintmin.presentation.ui.player_bar.PlayerBarIntent
import com.kintmin.presentation.ui.player_bar.PlayerBarUiState
import com.kintmin.presentation.ui.player_bar.PlayerBarViewModel
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderEvent
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderIntent
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderView
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListItemUiState
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderUiState
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderViewModel
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListEvent
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListIntent
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListEmptyView
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListItemView
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.min

@Composable
fun PlaylistDetailScreen(
    navigateToBack: () -> Unit,
    navigateToAddAudioMediaScreen: (playlistId: Int) -> Unit,
    navigateToPlaylistEditScreen: (playlistId: Int, focusAudioMediaId: Int?) -> Unit,
    navigateToAudioDetailScreen: (audioMediaId: Int) -> Unit,
    navigateToAudioEditScreen: (audioMediaId: Int) -> Unit,
    navigateToPlayerDetail: () -> Unit,
    focusAudioMediaId: Int? = null,
) {
    val headerViewModel = koinViewModel<PlaylistDetailHeaderViewModel>()
    val listViewModel = koinViewModel<PlaylistDetailListViewModel>()
    val playerBarViewModel = koinViewModel<PlayerBarViewModel>()

    val headerData by headerViewModel.headerDataFlow.collectAsState()
    val audioList by listViewModel.audioListFlow.collectAsState()
    val currentMediaItem by playerBarViewModel.currentMediaItem.collectAsState()

    LaunchedEffect(Unit) {
        headerViewModel.eventFlow.collect { event ->
            when (event) {
                PlaylistDetailHeaderEvent.NavigateToAddAudioMediaScreen -> navigateToAddAudioMediaScreen(headerViewModel.playlistId)
                PlaylistDetailHeaderEvent.NavigateToEditPlaylistScreen -> navigateToPlaylistEditScreen(headerViewModel.playlistId, null)
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
                    headerViewModel.playlistId,
                    event.focusAudioMediaId,
                )
                is PlaylistDetailListEvent.ShowToast -> toaster.show(event.message)
            }
        }
    }

    PlaylistDetailScreen(
        navigateToBack = navigateToBack,
        headerData = headerData,
        audioPlayDataList = audioList,
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
    navigateToBack: () -> Unit,
    headerData: PlaylistDetailHeaderUiState,
    audioPlayDataList: List<PlaylistDetailListItemUiState>,
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
    )
    var isSearchFieldVisible by rememberSaveable { mutableStateOf(false) }
    var searchInput by rememberSaveable { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    var consumedFocusAudioMediaId by rememberSaveable { mutableStateOf<Int?>(null) }
    var highlightedAudioMediaId by remember { mutableStateOf<Int?>(null) }
    var focusSpreadProgress by remember { mutableStateOf(0f) }
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
    val nowPlayingAudioMediaId = playerBar.id.toIntOrNull()
    val isPlaybackPlaying = playerBar.isPlaying

    LaunchedEffect(focusAudioMediaId, audioPlayDataList) {
        if (focusAudioMediaId == null || audioPlayDataList.isEmpty()) return@LaunchedEffect
        if (consumedFocusAudioMediaId == focusAudioMediaId) return@LaunchedEffect

        val targetIndexInList = audioPlayDataList.indexOfFirst { it.id == focusAudioMediaId }
        if (targetIndexInList < 0) return@LaunchedEffect

        val targetItemIndex = targetIndexInList + 1 // 0 is header
        val isTargetVisibleNow = scrollState.layoutInfo.visibleItemsInfo.any { it.index == targetItemIndex }
        if (!isTargetVisibleNow) {
            scrollState.scrollToItem(targetItemIndex)
        }

        val targetItemInfo = snapshotFlow {
            scrollState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetItemIndex }
        }.filter { it != null }
            .first() ?: return@LaunchedEffect

        val layoutInfo = scrollState.layoutInfo
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val itemCenter = targetItemInfo.offset + (targetItemInfo.size / 2)
        val deltaToCenter = (itemCenter - viewportCenter).toFloat()

        if (kotlin.math.abs(deltaToCenter) > 1f) {
            scrollState.scrollBy(deltaToCenter)
        }

        highlightedAudioMediaId = focusAudioMediaId
        focusSpreadProgress = 0f
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        ) { value, _ ->
            focusSpreadProgress = value
        }
        focusSpreadProgress = 0f
        highlightedAudioMediaId = null
        consumedFocusAudioMediaId = focusAudioMediaId
    }

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
                    IconButton(
                        onClick = { navigateToBack() },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            modifier = Modifier.size(topAppBarIconSize),
                            contentDescription = "뒤로",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val showSearchBar = !isSearchFieldVisible
                            isSearchFieldVisible = showSearchBar
                            if (!showSearchBar) {
                                searchInput = ""
                                sendPlaylistDetailListIntent(
                                    PlaylistDetailListIntent.OnChangeSearchText(""),
                                )
                            }
                        },
                    ) {
                        // 아이콘은 바꾸지 않는다. 바로 아래 텍스트필드에도 X 가 있어 둘이
                        // 같은 모양이면 어느 쪽이 무엇을 닫는지 읽히지 않는다.
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            modifier = Modifier.size(topAppBarIconSize),
                            contentDescription = if (isSearchFieldVisible) "검색 닫기" else "음원 검색",
                        )
                    }
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
                if (audioPlayDataList.isEmpty()) {
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
                                isNowPlaying = item.id == nowPlayingAudioMediaId,
                                isPlaying = isPlaybackPlaying,
                                isDragging = false,
                                sendIntent = sendPlaylistDetailListIntent,
                            )
                            if (highlightedAudioMediaId == item.id) {
                                FocusSpreadOverlay(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clipToBounds(),
                                    progress = focusSpreadProgress,
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
                                isNowPlaying = item.id == nowPlayingAudioMediaId,
                                isPlaying = isPlaybackPlaying,
                                isDragging = reorderState.draggingItemId == item.id,
                                sendIntent = sendPlaylistDetailListIntent,
                            )
                            if (highlightedAudioMediaId == item.id) {
                                FocusSpreadOverlay(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clipToBounds(),
                                    progress = focusSpreadProgress,
                                )
                            }
                        }
                    }
                }
            }

            // 상단 바 안에 두면 innerPadding 이 늘어 목록이 통째로 밀린다.
            AnimatedVisibility(
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                visible = isSearchFieldVisible,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                ) { -it } + fadeIn(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                ),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                ) { -it } + fadeOut(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                ),
            ) {
                // AnimatedVisibility 안에서 요청한다. 바깥에서 isSearchFieldVisible 로 걸면
                // 필드가 아직 붙지 않은 시점에 돌 수 있다.
                LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester)
                        // 상단 바는 헤더 위에서 투명하다가 스크롤하면 background 가 된다.
                        .background(
                            lerp(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                MaterialTheme.colorScheme.background,
                                progress,
                            ),
                        )
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        sendPlaylistDetailListIntent(
                            PlaylistDetailListIntent.OnChangeSearchText(it),
                        )
                    },
                    label = { Text("음원 제목 검색") },
                    singleLine = true,
                    trailingIcon = {
                        val isSearchInputEmpty = searchInput.isEmpty()
                        IconButton(
                            onClick = {
                                // 비어 있으면 지울 것이 없다. 그때는 닫는 것이 유일하게 뜻이 있다.
                                if (isSearchInputEmpty) {
                                    isSearchFieldVisible = false
                                } else {
                                    searchInput = ""
                                    sendPlaylistDetailListIntent(
                                        PlaylistDetailListIntent.OnChangeSearchText(""),
                                    )
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = if (isSearchInputEmpty) "검색 닫기" else "검색어 전체 삭제",
                            )
                        }
                    },
                )
            }
        }
    }
}

@Preview
@Composable
fun PlaylistDetailScreenPreview() {
    JellyTubeTheme {
        PlaylistDetailScreen(
            navigateToBack = {},
            headerData = PlaylistDetailHeaderUiState.getMock(),
            audioPlayDataList = PlaylistDetailListItemUiState.getMockList(),
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
            navigateToBack = {},
            headerData = PlaylistDetailHeaderUiState.getMock(),
            audioPlayDataList = emptyList(),
            isBasePlaylist = false,
            playerBar = PlayerBarUiState.getMock(),
            navigateToPlayerDetail = {},
            sendPlaylistDetailListIntent = {},
            sendPlaylistDetailHeaderIntent = {},
            sendPlayerBarIntent = {},
        )
    }
}

