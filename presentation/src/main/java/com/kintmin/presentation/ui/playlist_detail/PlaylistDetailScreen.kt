package com.kintmin.presentation.ui.playlist_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme
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

@Composable
fun PlaylistDetailScreen(
    navigateToBack: () -> Unit,
    navigateToAddAudioMediaScreen: () -> Unit,
    navigateToEditPlaylistScreen: () -> Unit,
    navigateToAudioDetailScreen: () -> Unit,
) {
    val headerViewModel = hiltViewModel<PlaylistDetailHeaderViewModel>()
    val listViewModel = hiltViewModel<PlaylistDetailListViewModel>()

    val headerData by headerViewModel.headerDataFlow.collectAsState()
    val audioList by listViewModel.audioListFlow.collectAsState()

    LaunchedEffect(Unit) {
        headerViewModel.eventFlow.collect { event ->
            when (event) {
                PlaylistDetailHeaderEvent.NavigateToAddAudioMediaScreen -> navigateToAddAudioMediaScreen()
                PlaylistDetailHeaderEvent.NavigateToEditPlaylistScreen -> navigateToEditPlaylistScreen()
            }
        }
    }

    LaunchedEffect(Unit) {
        listViewModel.eventFlow.collect { event ->
            when (event) {
                PlaylistDetailListEvent.NavigateToAudioDetailScreen -> navigateToAudioDetailScreen()
            }
        }
    }

    PlaylistDetailScreen(
        navigateToBack = navigateToBack,
        headerData = headerData,
        audioPlayDataList = audioList,
        isBasePlaylist = headerViewModel.isBasePlaylist,
        sendPlaylistDetailListIntent = listViewModel::sendIntent,
        sendPlaylistDetailHeaderIntent = headerViewModel::sendIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    navigateToBack: () -> Unit,
    headerData: PlaylistDetailHeaderUiState,
    audioPlayDataList: List<PlaylistDetailListItemUiState>,
    isBasePlaylist: Boolean,
    sendPlaylistDetailListIntent: (PlaylistDetailListIntent) -> Unit,
    sendPlaylistDetailHeaderIntent: (PlaylistDetailHeaderIntent) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val audioPlayList = remember { mutableStateListOf<PlaylistDetailListItemUiState>() }
    var draggingItemId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(audioPlayDataList) {
        audioPlayList.clear()
        audioPlayList.addAll(audioPlayDataList)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navigateToBack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "ArrowBackIosNew"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                PlaylistDetailHeaderView(
                    headerData = headerData,
                    sendIntent = sendPlaylistDetailHeaderIntent,
                )
            }
            itemsIndexed(
                items = audioPlayList,
                key = { _, item -> item.id }
            ) { index, item ->
                Box(
                    modifier =  Modifier
                        .animateItem()
                        .zIndex(1f.takeIf { item.id == draggingItemId } ?: 0f)
                        //.background(Color(0xFFF7F7F7).takeIf { item.id == draggingItemId } ?: Color.White)
                        .drawBehind {
                            if (item.id == draggingItemId) {
                                drawLine(
                                    color = Color(0xFFDADADA),
                                    strokeWidth = 0.5.dp.toPx(),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f)
                                )
                                drawLine(
                                    color = Color(0xFFDADADA),
                                    strokeWidth = 0.5.dp.toPx(),
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height)
                                )
                            }
                        }
                        .pointerInput(item.id, item.sequence) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragOffset = 0f
                                    draggingItemId = item.id
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y

                                    val itemHeightPx = with(density) { 56.dp.toPx() }
                                    val moved = (dragOffset / itemHeightPx).toInt()

                                    val fromIndex = audioPlayList.indexOfFirst { it.id == draggingItemId }
                                    val toIndex = (fromIndex + moved).coerceIn(0, audioPlayList.lastIndex)

                                    if (fromIndex != toIndex) {
                                        audioPlayList.add(toIndex, audioPlayList.removeAt(fromIndex))
                                        dragOffset -= (moved * itemHeightPx)
                                    }
                                },
                                onDragEnd = {
                                    val fromIndex = audioPlayDataList.indexOfFirst { it.id == item.id }
                                    val toIndex = audioPlayList.indexOfFirst { it.id == draggingItemId }
                                    sendPlaylistDetailListIntent(
                                        PlaylistDetailListIntent.ReorderAudioItem(
                                            reorderData = item,
                                            targetData = audioPlayDataList[toIndex]
                                        ),
                                    )
                                    draggingItemId = null
                                },
                            )
                        }
                ) {
                    PlaylistDetailListItemView(
                        data = item,
                        isBasePlaylist = isBasePlaylist,
                        sendIntent = sendPlaylistDetailListIntent,
                    )
                }
            }
        }
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
            isBasePlaylist = true,
            sendPlaylistDetailListIntent = {},
            sendPlaylistDetailHeaderIntent = {},
        )
    }
}