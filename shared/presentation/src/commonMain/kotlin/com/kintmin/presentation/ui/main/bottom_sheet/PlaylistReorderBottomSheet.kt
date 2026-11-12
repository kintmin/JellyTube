package com.kintmin.presentation.ui.main.bottom_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.JellyTubeBottomSheet
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState
import com.kintmin.presentation.remember_state.rememberReorderState

private val ITEM_HEIGHT = 56.dp

@Composable
fun PlaylistReorderBottomSheet(
    playlists: List<PlaylistItemUiState>,
    onReorder: (orderedIds: List<Int>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderState(
        listState = listState,
        dataList = playlists,
        idOf = { it.id },
    )

    LaunchedEffect(playlists) {
        reorderState.items.clear()
        reorderState.items.addAll(playlists)
    }

    JellyTubeBottomSheet(
        onDismissRequest = onDismissRequest,
        // 드래그 중에는 시트 자체 드래그를 잠가 reorder 제스처와의 충돌을 막는다.
        sheetGesturesEnabled = reorderState.draggingItemId == null,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
        ) {
            itemsIndexed(
                items = reorderState.items,
                key = { _, item -> item.id },
            ) { _, item ->
                PlaylistReorderItemView(
                    modifier = Modifier.animateItem(),
                    data = item,
                    draggingItemId = reorderState.draggingItemId,
                    onDragStart = reorderState::onDragStart,
                    onDrag = reorderState::onDrag,
                    onDragEnd = {
                        if (reorderState.getDraggingItemIndex() != null) {
                            onReorder(reorderState.items.map { it.id })
                        }
                        reorderState.onDragEnd()
                    },
                )
            }
        }
    }
}

@Composable
private fun PlaylistReorderItemView(
    modifier: Modifier,
    data: PlaylistItemUiState,
    draggingItemId: Int?,
    onDragStart: (Offset, Int) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ITEM_HEIGHT)
            .zIndex(1f.takeIf { data.id == draggingItemId } ?: 0f)
            .background(
                if (data.id == draggingItemId) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = data.name,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        IconButton(
            modifier = Modifier
                .pointerInput(data.id, data.sequence) {
                    detectDragGestures(
                        onDragStart = { onDragStart(it, data.id) },
                        onDrag = { change, dragAmount -> onDrag(change, dragAmount) },
                        onDragEnd = { onDragEnd() },
                    )
                },
            onClick = {},
        ) {
            Icon(
                imageVector = Icons.Rounded.Reorder,
                contentDescription = "순서 변경 핸들",
            )
        }
    }
}

@Preview
@Composable
private fun PlaylistReorderBottomSheetPreview() {
    JellyTubeTheme {
        PlaylistReorderItemView(
            modifier = Modifier,
            data = PlaylistItemUiState.getMock(id = 1),
            draggingItemId = null,
            onDragStart = { _, _ -> },
            onDrag = { _, _ -> },
            onDragEnd = {},
        )
    }
}
