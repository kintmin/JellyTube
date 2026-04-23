package com.kintmin.presentation.ui.playlist_detail

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Composable
fun PlaylistDetailScreen(
    navigateToBack: () -> Unit,
    navigateToAddAudioMediaScreen: (playlistId: Int) -> Unit,
    navigateToPlaylistEditScreen: (playlistId: Int) -> Unit,
    navigateToAudioDetailScreen: (audioMediaId: Int) -> Unit,
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
    var focusBorderProgress by remember { mutableStateOf(0f) }
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
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FocusTraceEasing),
        ) { value, _ ->
            focusBorderProgress = value
        }
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
                val flashEnvelope = if (focusBorderProgress <= 0.5f) {
                    focusBorderProgress * 2f
                } else {
                    (1f - focusBorderProgress) * 2f
                }
                val flashAlpha = 0.14f * flashEnvelope
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
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.White.copy(alpha = flashAlpha))
                        )
                        FocusTraceBorder(
                            modifier = Modifier
                                .matchParentSize()
                                ,
                            progress = focusBorderProgress,
                        )
                    }
                }
            }
        }
    }
}

private const val FOCUS_TRACE_START_SPEED_RATIO = 0.12f
private const val FOCUS_TRACE_PEAK_SPEED_MULTIPLIER = 3f

private val FocusTraceEasing = Easing { fraction ->
    val t = fraction.coerceIn(0f, 1f)
    val startSpeedRatio = FOCUS_TRACE_START_SPEED_RATIO.coerceIn(0f, 0.95f)
    val peakMultiplier = FOCUS_TRACE_PEAK_SPEED_MULTIPLIER.coerceAtLeast(1f)

    // 중앙 burst를 더 다이나믹하게 만드는 비선형 지수.
    // piecewise power easing의 중앙 최대 속도는 p 이므로, 원하는 배수에 맞게 역산.
    val power = ((peakMultiplier - startSpeedRatio) / (1f - startSpeedRatio))
        .coerceAtLeast(1.05f)

    val burst = if (t <= 0.5f) {
        0.5f * (2f * t).pow(power)
    } else {
        1f - 0.5f * (2f * (1f - t)).pow(power)
    }

    // 시작/끝 기본 속도 + 중앙 burst 합성
    (startSpeedRatio * t) + ((1f - startSpeedRatio) * burst)
}

@Composable
private fun FocusTraceBorder(
    modifier: Modifier = Modifier,
    progress: Float,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val traceColor = Color.White.copy(alpha = 0.95f)
        val width = size.width
        val height = size.height
        val perimeter = (2f * (width + height)).coerceAtLeast(1f)
        val clampedProgress = progress.coerceIn(0f, 1f)
        val lengthEnvelope = if (clampedProgress <= 0.5f) {
            clampedProgress * 2f
        } else {
            (1f - clampedProgress) * 2f
        }
        val traceLength = perimeter * 0.22f * lengthEnvelope

        val head = clampedProgress * perimeter
        val tail = max(0f, head - traceLength)
        drawRectPathSegment(
            width = width,
            height = height,
            startDistance = tail,
            endDistance = head,
            color = traceColor,
            strokeWidth = strokeWidth,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRectPathSegment(
    width: Float,
    height: Float,
    startDistance: Float,
    endDistance: Float,
    color: Color,
    strokeWidth: Float,
) {
    if (endDistance <= startDistance) return
    val perimeter = (2f * (width + height)).coerceAtLeast(1f)
    var cursor = startDistance.coerceIn(0f, perimeter)
    val target = endDistance.coerceIn(0f, perimeter)
    while (cursor < target) {
        val edgeEnd = min(nextCornerDistance(cursor, width, height), target)
        drawLine(
            color = color,
            start = pointAtDistance(cursor, width, height),
            end = pointAtDistance(edgeEnd, width, height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square,
        )
        cursor = edgeEnd
    }
}

private fun pointAtDistance(distance: Float, width: Float, height: Float): Offset {
    val top = width
    val right = top + height
    val bottom = right + width
    return when {
        distance <= top -> Offset(distance, 0f)
        distance <= right -> Offset(width, distance - top)
        distance <= bottom -> Offset(width - (distance - right), height)
        else -> Offset(0f, height - (distance - bottom))
    }
}

private fun nextCornerDistance(distance: Float, width: Float, height: Float): Float {
    val top = width
    val right = top + height
    val bottom = right + width
    val perimeter = bottom + height
    return when {
        distance < top -> top
        distance < right -> right
        distance < bottom -> bottom
        else -> perimeter
    }
}

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
