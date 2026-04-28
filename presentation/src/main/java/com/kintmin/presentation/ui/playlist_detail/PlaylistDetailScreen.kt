package com.kintmin.presentation.ui.playlist_detail

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme
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
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListItemView
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun PlaylistDetailScreen(
    navigateToBack: () -> Unit,
    navigateToAddAudioMediaScreen: (playlistId: Int) -> Unit,
    navigateToPlaylistEditScreen: (playlistId: Int) -> Unit,
    navigateToAudioDetailScreen: (audioMediaId: Int) -> Unit,
    navigateToAudioEditScreen: (audioMediaId: Int) -> Unit,
    navigateToPlayerDetail: () -> Unit,
    focusAudioMediaId: Int? = null,
) {
    val headerViewModel = hiltViewModel<PlaylistDetailHeaderViewModel>()
    val listViewModel = hiltViewModel<PlaylistDetailListViewModel>()
    val playerBarViewModel = hiltViewModel<PlayerBarViewModel>()

    val headerData by headerViewModel.headerDataFlow.collectAsState()
    val audioList by listViewModel.audioListFlow.collectAsState()
    val currentMediaItem by playerBarViewModel.currentMediaItem.collectAsState()

    LaunchedEffect(Unit) {
        headerViewModel.eventFlow.collect { event ->
            when (event) {
                PlaylistDetailHeaderEvent.NavigateToAddAudioMediaScreen -> navigateToAddAudioMediaScreen(headerViewModel.playlistId)
                PlaylistDetailHeaderEvent.NavigateToEditPlaylistScreen -> navigateToPlaylistEditScreen(headerViewModel.playlistId)
            }
        }
    }

    LaunchedEffect(Unit) {
        listViewModel.eventFlow.collect { event ->
            when (event) {
                is PlaylistDetailListEvent.NavigateToAudioDetailScreen -> navigateToAudioDetailScreen(event.audioMediaId)
                is PlaylistDetailListEvent.NavigateToAudioEditScreen -> navigateToAudioEditScreen(event.audioMediaId)
            }
        }
    }

    PlaylistDetailScreen(
        navigateToBack = navigateToBack,
        headerData = headerData,
        audioPlayDataList = audioList,
        isBasePlaylist = headerViewModel.isBasePlaylist,
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
            Column {
                TopAppBar(
                    title = {
                        Text(
                            headerData.name,
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = progress)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigateToBack() },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBackIosNew,
                                contentDescription = "ArrowBackIosNew"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor
                    )
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
                Box(
                    modifier = Modifier
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        }
    ) { innerPadding ->
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
            itemsIndexed(
                items = audioPlayDataList,
                key = { _, item -> item.id }
            ) { _, item ->
                val isHighlighted = highlightedAudioMediaId == item.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .animateItem()
                ) {
                    PlaylistDetailListItemView(
                        data = item,
                        modifier = Modifier.matchParentSize(),
                        isBasePlaylist = isBasePlaylist,
                        sendIntent = sendPlaylistDetailListIntent,
                    )
                    if (isHighlighted) {
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
}

@Composable
private fun FocusSpreadOverlay(
    modifier: Modifier = Modifier,
    progress: Float,
) {
    val primary = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        val clamped = progress.coerceIn(0f, 1f)
        val envelope = (1f - abs((2f * clamped) - 1f)).coerceIn(0f, 1f)
        if (envelope <= 0f) return@Canvas

        val width = size.width
        val height = size.height
        val center = Offset(width * 0.5f, height * 0.54f)
        val diagonal = hypot(width, height)
        val radius = diagonal * (0.24f + (0.84f * clamped))

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.15f * envelope),
                    accent.copy(alpha = 0.08f * envelope),
                    Color.Transparent,
                ),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
        val scaleRadius = height * 0.20f
        val rowStep = scaleRadius * 1.18f
        val colStep = scaleRadius * 1.86f
        val phase = clamped * TAU * 2.2f
        val drift = width * (0.10f + (0.16f * clamped))
        val scalePath = Path()

        var rowIndex = 0
        var y = -scaleRadius
        while (y <= height + scaleRadius) {
            val rowOffset = if (rowIndex % 2 == 0) 0f else scaleRadius * 0.93f
            var x = (-2f * scaleRadius) + rowOffset + drift
            while (x <= width + (2f * scaleRadius)) {
                val wave = ((sin((x / width) * TAU * 1.8f + (y / height) * TAU * 0.7f + phase) + 1f) * 0.5f)
                    .coerceIn(0f, 1f)
                val alpha = (0.05f + (0.22f * wave)) * envelope
                val tint = lerp(primary, accent, wave)

                scalePath.reset()
                scalePath.moveTo(x - scaleRadius, y)
                scalePath.quadraticTo(x, y - (scaleRadius * 0.90f), x + scaleRadius, y)
                scalePath.quadraticTo(x, y + (scaleRadius * 0.56f), x - scaleRadius, y)
                scalePath.close()

                drawPath(
                    path = scalePath,
                    color = tint.copy(alpha = alpha),
                )
                x += colStep
            }
            y += rowStep
            rowIndex += 1
        }
    }
}

private const val TAU = 6.2831855f

@Preview(showBackground = true)
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
