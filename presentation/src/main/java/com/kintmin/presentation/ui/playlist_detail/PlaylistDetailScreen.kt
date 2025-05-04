package com.kintmin.presentation.ui.playlist_detail

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
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
import com.kintmin.presentation.ui.playlist_detail.list.reorder.rememberReorderState

@Composable
fun PlaylistDetailScreen(
    navigateToBack: () -> Unit,
    navigateToAddAudioMediaScreen: () -> Unit,
    navigateToAudioDetailScreen: () -> Unit,
) {
    val headerViewModel = hiltViewModel<PlaylistDetailHeaderViewModel>()
    val listViewModel = hiltViewModel<PlaylistDetailListViewModel>()

    val headerData by headerViewModel.headerDataFlow.collectAsState()
    val isEditMode by headerViewModel.isEditMode.collectAsState()
    val audioList by listViewModel.audioListFlow.collectAsState()

    LaunchedEffect(Unit) {
        headerViewModel.eventFlow.collect { event ->
            when (event) {
                PlaylistDetailHeaderEvent.NavigateToAddAudioMediaScreen -> navigateToAddAudioMediaScreen()
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
        isEditMode = isEditMode,
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
    isEditMode: Boolean,
    sendPlaylistDetailListIntent: (PlaylistDetailListIntent) -> Unit,
    sendPlaylistDetailHeaderIntent: (PlaylistDetailHeaderIntent) -> Unit,
) {
    val reorderState = rememberReorderState(
        audioPlayDataList = audioPlayDataList,
        initializeItemHeightPx = 80.dp,
    )

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
            state = reorderState.listState,
        ) {
            item {
                PlaylistDetailHeaderView(
                    headerData = headerData,
                    sendIntent = sendPlaylistDetailHeaderIntent,
                )
            }
            itemsIndexed(
                items = reorderState.audioPlayList,
                key = { _, item -> item.id }
            ) { _, item ->
                Box(
                    modifier = Modifier.animateItem()
                ) {
                    PlaylistDetailListItemView(
                        data = item,
                        modifier = Modifier.height(80.dp),
                        isEditMode = isEditMode,
                        isBasePlaylist = isBasePlaylist,
                        draggingItemId = reorderState.draggingItemId,
                        onDragStart = reorderState::onDragStart,
                        onDrag = reorderState::onDrag,
                        onDragEnd = {
                            reorderState.getDraggingItemIndex()?.let {
                                sendPlaylistDetailListIntent(
                                    PlaylistDetailListIntent.ReorderAudioItem(
                                        reorderData = item,
                                        targetData = audioPlayDataList[it]
                                    ),
                                )
                            }
                            reorderState.onDragEnd()
                        },
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
            isEditMode = false,
            sendPlaylistDetailListIntent = {},
            sendPlaylistDetailHeaderIntent = {},
        )
    }
}