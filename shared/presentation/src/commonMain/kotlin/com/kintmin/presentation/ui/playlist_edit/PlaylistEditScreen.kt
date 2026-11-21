package com.kintmin.presentation.ui.playlist_edit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kintmin.presentation.animation.FocusSpreadOverlay
import com.kintmin.presentation.platform.rememberImagePicker
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.remember_state.rememberReorderState
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.destructiveContent
import com.kintmin.presentation.theme.topAppBarDetailTitleFontSize
import com.kintmin.presentation.theme.topAppBarDetailTitleLineHeight
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.theme.topAppBarIconSize
import com.kintmin.presentation.ui.common.BottomActionBar
import com.kintmin.presentation.ui.common.BottomActionButton
import com.kintmin.presentation.ui.common.ReorderableItem
import com.kintmin.presentation.ui.common.dragToSelect
import com.kintmin.presentation.ui.playlist_edit.dialog.DeleteFullAudioMediaListDialog
import com.kintmin.presentation.ui.playlist_edit.header.PlaylistEditHeaderUiState
import com.kintmin.presentation.ui.playlist_edit.header.PlaylistEditHeaderView
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListEvent
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListIntent
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListItemUiState
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListItemView
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    var consumedFocusAudioMediaId by rememberSaveable { mutableStateOf<Int?>(null) }
    var highlightedAudioMediaId by remember { mutableStateOf<Int?>(null) }
    var focusSpreadProgress by remember { mutableFloatStateOf(0f) }

    var isShowDialog by remember { mutableStateOf(false) }

    LaunchedEffect(focusAudioMediaId, dataList) {
        if (focusAudioMediaId == null || dataList.isEmpty()) return@LaunchedEffect
        if (consumedFocusAudioMediaId == focusAudioMediaId) return@LaunchedEffect

        val targetIndexInList = dataList.indexOfFirst { it.id == focusAudioMediaId }
        if (targetIndexInList < 0) return@LaunchedEffect

        val targetItemIndex = targetIndexInList + 1 // 0 is header
        val isTargetVisibleNow = scrollState.layoutInfo.visibleItemsInfo.any { it.index == targetItemIndex }
        if (!isTargetVisibleNow) {
            scrollState.scrollToItem(targetItemIndex)
        }

        val targetItemInfo = snapshotFlow {
            scrollState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetItemIndex }
        }.filter { it != null }.first() ?: return@LaunchedEffect

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

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = checkedItemCount > 0,
    ) {
        sendIntent(PlaylistEditListIntent.OnClickClearCheckedItemList)
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
            val isSelecting = checkedItemCount > 0
            TopAppBar(
                expandedHeight = topAppBarHeight,
                title = {
                    Text(
                        text = if (isSelecting) "${checkedItemCount}곡 선택됨" else "플레이리스트 편집",
                        fontSize = topAppBarDetailTitleFontSize,
                        lineHeight = topAppBarDetailTitleLineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (isSelecting) {
                        IconButton(onClick = { sendIntent(PlaylistEditListIntent.OnClickClearCheckedItemList) }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                modifier = Modifier.size(topAppBarIconSize),
                                contentDescription = "선택 해제",
                            )
                        }
                    } else {
                        IconButton(onClick = { navigateToBack() }) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBackIosNew,
                                modifier = Modifier.size(topAppBarIconSize),
                                contentDescription = "ArrowBackIosNew"
                            )
                        }
                    }
                },
                actions = {
                    if (isSelecting) {
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
                        onClick = { sendIntent(PlaylistEditListIntent.OnClickDeleteAudioMediaListInPlaylist) },
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

