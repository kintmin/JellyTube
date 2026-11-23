package com.kintmin.presentation.ui.playlist_edit

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.animation.FocusSpreadOverlay
import com.kintmin.presentation.platform.rememberImagePicker
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.remember_state.rememberFocusSpreadState
import com.kintmin.presentation.remember_state.rememberReorderState
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.destructiveContent
import com.kintmin.presentation.ui.common.BottomActionBar
import com.kintmin.presentation.ui.common.BottomActionButton
import com.kintmin.presentation.ui.common.ReorderableItem
import com.kintmin.presentation.ui.common.SelectionTopAppBar
import com.kintmin.presentation.ui.common.dragToSelect
import com.kintmin.presentation.ui.playlist_detail.dialog.RemoveAudioMediaFromPlaylistDialog
import com.kintmin.presentation.ui.playlist_edit.dialog.DeleteFullAudioMediaListDialog
import com.kintmin.presentation.ui.playlist_edit.header.PlaylistEditHeaderUiState
import com.kintmin.presentation.ui.playlist_edit.header.PlaylistEditHeaderView
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListEvent
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListIntent
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListItemUiState
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListItemView
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListViewModel
import kotlinx.coroutines.flow.filter
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlaylistEditScreen(
    navigateToBack: () -> Unit,
    focusAudioMediaId: Int? = null,
) {
    val mainViewModel = koinViewModel<PlaylistEditListViewModel>()

    val uiState by mainViewModel.uiState.collectAsState()
    val toaster = rememberToaster()
    val imagePicker = rememberImagePicker { imageData ->
        mainViewModel.sendIntent(PlaylistEditListIntent.OnPickPlaylistImage(imageData))
    }

    LaunchedEffect(Unit) {
        mainViewModel.eventFlow.collect { event ->
            when (event) {
                is PlaylistEditListEvent.ShowToast -> toaster.show(event.message)
            }
        }
    }

    PlaylistEditScreen(
        navigateToBack = navigateToBack,
        onClickPickImage = { imagePicker.launch() },
        isBasePlaylist = uiState.headerData.isBasePlaylist,
        headerData = uiState.headerData,
        dataList = uiState.audioMediaList,
        checkedItemCount = uiState.checkedItemCount,
        focusAudioMediaId = focusAudioMediaId,
        sendIntent = mainViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistEditScreen(
    navigateToBack: () -> Unit,
    onClickPickImage: () -> Unit,
    isBasePlaylist: Boolean,
    headerData: PlaylistEditHeaderUiState,
    dataList: List<PlaylistEditListItemUiState>,
    checkedItemCount: Int,
    focusAudioMediaId: Int? = null,
    sendIntent: (PlaylistEditListIntent) -> Unit,
) {
    val scrollState = rememberLazyListState()
    val reorderState = rememberReorderState(
        listState = scrollState,
        dataList = dataList,
        idOf = { it.id },
    )
    val focusSpread = rememberFocusSpreadState(
        listState = scrollState,
        focusId = focusAudioMediaId,
        ids = dataList.map { it.id },
        indexOffset = 1,
    )

    var isShowDialog by remember { mutableStateOf(false) }
    var isShowRemoveDialog by remember { mutableStateOf(false) }

    DeleteFullAudioMediaListDialog(
        isShow = isShowDialog,
        onDismiss = { isShowDialog = false },
        selectedMediaCount = checkedItemCount,
        deleteAudioMediaList = { sendIntent(PlaylistEditListIntent.OnClickFullDeleteAudioMediaList) },
    )

    RemoveAudioMediaFromPlaylistDialog(
        showDialog = isShowRemoveDialog,
        onDismiss = { isShowRemoveDialog = false },
        removeFromPlaylist = { sendIntent(PlaylistEditListIntent.OnClickDeleteAudioMediaListInPlaylist) },
        selectedMediaCount = checkedItemCount,
        isSecretPlaylist = true,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SelectionTopAppBar(
                title = "플레이리스트 편집",
                selectedCount = checkedItemCount,
                onClickBack = { navigateToBack() },
                onClearSelection = { sendIntent(PlaylistEditListIntent.OnClickClearCheckedItemList) },
                actions = {
                    if (checkedItemCount > 0) {
                        // 휴지통만 두면 "플레이리스트에서 빼기" 로 읽힐 수 있어 글자로 못 박는다.
                        TextButton(
                            modifier = Modifier.padding(end = 4.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = destructiveContent),
                            onClick = { isShowDialog = true },
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                            )
                            Text(
                                modifier = Modifier.padding(start = 4.dp),
                                text = "파일 삭제",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (checkedItemCount > 0 && !isBasePlaylist) {
                BottomActionBar {
                    BottomActionButton(
                        text = "${checkedItemCount}곡 플레이리스트에서 빼기",
                        // 비밀에서 빼면 곡이 드러나는 곳으로 간다. 그때만 묻는다.
                        onClick = {
                            if (headerData.isSecretPlaylist) isShowRemoveDialog = true
                            else sendIntent(PlaylistEditListIntent.OnClickDeleteAudioMediaListInPlaylist)
                        },
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .dragToSelect(
                    listState = scrollState,
                    selectableIds = reorderState.items.map { it.id },
                    checkedIds = dataList.filter { it.isChecked }.map { it.id }.toSet(),
                    onCheckedIdsChange = { sendIntent(PlaylistEditListIntent.OnDragSelectItems(it)) },
                ),
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
            state = scrollState,
        ) {
            item(
                key = -headerData.id,
            ) {
                PlaylistEditHeaderView(
                    data = headerData,
                    onClickPickImage = onClickPickImage,
                    sendIntent = sendIntent,
                )
            }

            itemsIndexed(
                items = reorderState.items,
                key = { _, item -> item.id },
            ) { _, item ->
                ReorderableItem(
                    state = reorderState,
                    id = item.id,
                    modifier = Modifier.height(80.dp),
                    onDragEnd = {
                        if (!reorderState.isOrderChanged) return@ReorderableItem
                        reorderState.getDraggingItemIndex()?.let {
                            sendIntent(
                                PlaylistEditListIntent.ReorderAudioItem(
                                    reorderData = item,
                                    targetData = dataList[it]
                                ),
                            )
                        }
                    },
                ) { dragHandleModifier ->
                    PlaylistEditListItemView(
                        data = item,
                        modifier = Modifier.fillMaxSize(),
                        sendIntent = sendIntent,
                        dragHandleModifier = dragHandleModifier,
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
}

@Preview
@Composable
fun PlaylistEditScreenPreview() {
    JellyTubeTheme {
        PlaylistEditScreen(
            navigateToBack = {},
            onClickPickImage = {},
            isBasePlaylist = false,
            headerData = PlaylistEditHeaderUiState.getMock(),
            dataList = PlaylistEditListItemUiState.getMockList(),
            checkedItemCount = PlaylistEditListItemUiState.getMockList().count { it.isChecked },
            sendIntent = {},
        )
    }
}

