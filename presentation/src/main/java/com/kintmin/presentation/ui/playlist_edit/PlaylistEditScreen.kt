package com.kintmin.presentation.ui.playlist_edit

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray10
import com.kintmin.presentation.theme.gray40
import com.kintmin.presentation.ui.playlist_edit.dialog.DeleteFullAudioMediaListDialog
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

    PlaylistEditScreen(
        navigateToBack = navigateToBack,
        isBasePlaylist = mainViewModel.isBasePlaylist,
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
    isBasePlaylist: Boolean,
    headerData: PlaylistEditHeaderUiState,
    dataList: List<PlaylistEditListItemUiState>,
    checkedItemCount: Int,
    sendIntent: (PlaylistEditListIntent) -> Unit,
) {
    val reorderState = rememberReorderState(
        audioPlayDataList = dataList,
        initializeItemHeightPx = 80.dp,
    )

    var isShowDialog by remember { mutableStateOf(false) }

    LaunchedEffect(dataList) {
        reorderState.audioPlayList.clear()
        reorderState.audioPlayList.addAll(dataList)
    }

    DeleteFullAudioMediaListDialog(
        isShow = isShowDialog,
        onDismiss = { isShowDialog = false },
        selectedMediaCount = checkedItemCount,
        deleteAudioMediaList = { sendIntent(PlaylistEditListIntent.OnClickFullDeleteAudioMediaList) },
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
                    TextButton(onClick = { sendIntent(PlaylistEditListIntent.OnClickClearCheckedItemList) }) {
                        Text(
                            text = "$checkedItemCount 선택 해제",
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (checkedItemCount > 0) {
                Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            shape = RectangleShape,
                            onClick = {
                                isShowDialog = true
                            }) {
                            Text(
                                text = "음원 삭제",
                                fontSize = 14.sp,
                            )
                        }
                        if (!isBasePlaylist) {
                            TextButton(
                                modifier = Modifier
                                    .weight(2f)
                                    .fillMaxSize(),
                                shape = RectangleShape,
                                onClick = { sendIntent(PlaylistEditListIntent.OnClickDeleteAudioMediaListInPlaylist) }) {
                                Text(
                                    text = "플레이리스트에서 삭제",
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .background(MaterialTheme.colorScheme.surface),
                        )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = reorderState.listState,
        ) {
            item(
                key = -headerData.id,
            ) {
                PlaylistEditHeaderView(
                    data = headerData,
                    sendIntent = sendIntent,
                )
            }

            itemsIndexed(
                items = reorderState.audioPlayList,
                key = { _, item -> item.id },
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
                                        targetData = reorderState.audioPlayList[it]
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
            isBasePlaylist = false,
            headerData = PlaylistEditHeaderUiState.getMock(),
            dataList = PlaylistEditListItemUiState.getMockList(),
            checkedItemCount = PlaylistEditListItemUiState.getMockList().count { it.isChecked },
            sendIntent = {},
        )
    }
}
