package com.kintmin.presentation.ui.playlist_edit

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.playlist_edit.header.PlaylistEditHeaderUiState
import com.kintmin.presentation.ui.playlist_edit.header.PlaylistEditHeaderView
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListIntent
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListItemUiState
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListItemView
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListViewModel
import com.kintmin.presentation.ui.playlist_edit.list.reorder.rememberReorderState

@Composable
fun PlaylistEditScreen(
    navigateToBack: () -> Unit,
) {
    val mainViewModel = hiltViewModel<PlaylistEditListViewModel>()

    val audioMediaList by mainViewModel.audioMediaListFlow.collectAsState()
    val checkedItemCount by mainViewModel.checkedItemCountFlow.collectAsState()
    val headerData by mainViewModel.headerDataFlow.collectAsState()

    LaunchedEffect(Unit) {
        mainViewModel.eventFlow.collect { event ->
            when (event) {
                else -> {}
            }
        }
    }

    PlaylistEditScreen(
        navigateToBack = navigateToBack,
        headerData = headerData,
        dataList = audioMediaList,
        checkedItemCount = checkedItemCount,
        sendIntent = mainViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistEditScreen(
    navigateToBack: () -> Unit,
    headerData: PlaylistEditHeaderUiState,
    dataList: List<PlaylistEditListItemUiState>,
    checkedItemCount: Int,
    sendIntent: (PlaylistEditListIntent) -> Unit,
) {
    val reorderState = rememberReorderState(
        audioPlayDataList = dataList,
        initializeItemHeightPx = 80.dp,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "플레이리스트 편집",
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigateToBack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "ArrowBackIosNew"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {}) {
                        Text(
                            text = "$checkedItemCount 선택 해제",
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
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
                PlaylistEditHeaderView(headerData)
            }

            itemsIndexed(
                items = reorderState.audioPlayList,
                key = { _, item -> item.id }
            ) { _, item ->
                Box(modifier = Modifier.animateItem()) {
                    PlaylistEditListItemView(
                        data = item,
                        modifier = Modifier.height(80.dp),
                        sendIntent = sendIntent,
                        draggingItemId = reorderState.draggingItemId,
                        onDragStart = reorderState::onDragStart,
                        onDrag = reorderState::onDrag,
                        onDragEnd = {
                            reorderState.getDraggingItemIndex()?.let {
                                sendIntent(
                                    PlaylistEditListIntent.ReorderAudioItem(
                                        reorderData = item,
                                        targetData = dataList[it]
                                    ),
                                )
                            }
                            reorderState.onDragEnd()
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistEditScreenPreview() {
    JellyTubeTheme {
        PlaylistEditScreen(
            navigateToBack = {},
            headerData = PlaylistEditHeaderUiState.getMock(),
            dataList = PlaylistEditListItemUiState.getMockList(),
            checkedItemCount = PlaylistEditListItemUiState.getMockList().count { it.isChecked },
            sendIntent = {},
        )
    }
}