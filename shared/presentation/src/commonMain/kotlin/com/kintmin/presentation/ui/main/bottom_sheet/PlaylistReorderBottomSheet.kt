package com.kintmin.presentation.ui.main.bottom_sheet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.remember_state.rememberReorderState
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.ui.common.JellyTubeBottomSheet
import com.kintmin.presentation.ui.common.ReorderableItem
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState

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

    fun commitOrder() = onReorder(reorderState.items.map { it.id })

    JellyTubeBottomSheet(
        onDismissRequest = onDismissRequest,
        // 드래그 중에는 시트 자체 드래그를 잠가 reorder 제스처와의 충돌을 막는다.
        sheetGesturesEnabled = reorderState.draggingItemId == null,
    ) {
        // 머리말을 Column 으로 감싸면 LazyColumn 이 높이 제약을 잃는다. 같은 리스트에 넣는다.
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
        ) {
            item(key = "header") {
                // 범위는 제목이 말한다. 이 시트는 내 플레이리스트 머리말에서만 열리고
                // 라이브러리는 목록에 아예 없으므로 따로 설명할 것이 없다.
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    text = "내 플레이리스트 순서",
                    // titleMedium 은 항목의 bodyLarge 와 같은 16sp 라 제목으로 안 읽힌다.
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            itemsIndexed(
                items = reorderState.items,
                key = { _, item -> item.id },
            ) { index, item ->
                ReorderableItem(
                    state = reorderState,
                    id = item.id,
                    modifier = Modifier.height(ITEM_HEIGHT),
                    onDragEnd = { if (reorderState.isOrderChanged) commitOrder() },
                ) { dragHandleModifier ->
                    PlaylistReorderItemView(
                        data = item,
                        dragHandleModifier = dragHandleModifier,
                        canMoveUp = index > 0,
                        canMoveDown = index < reorderState.items.lastIndex,
                        onMoveUp = {
                            reorderState.items.add(index - 1, reorderState.items.removeAt(index))
                            commitOrder()
                        },
                        onMoveDown = {
                            reorderState.items.add(index + 1, reorderState.items.removeAt(index))
                            commitOrder()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistReorderItemView(
    data: PlaylistItemUiState,
    dragHandleModifier: Modifier,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = data.name,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // 드래그 말고도 보이는 길을 둔다. 제스처만으로 닿는 기능은 알 길이 없다.
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "위로 옮기기",
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "아래로 옮기기",
            )
        }

        // 눌러도 아무 일이 없는 IconButton 이 아니라, 끌기만 받는 자리로 둔다.
        Icon(
            modifier = dragHandleModifier
                .size(48.dp)
                .padding(12.dp),
            imageVector = Icons.Rounded.Reorder,
            contentDescription = "끌어서 순서 변경",
            tint = gray60,
        )
    }
}

@Preview
@Composable
private fun PlaylistReorderBottomSheetPreview() {
    JellyTubeTheme {
        PlaylistReorderItemView(
            data = PlaylistItemUiState.getMock(id = 1),
            dragHandleModifier = Modifier,
            canMoveUp = false,
            canMoveDown = true,
            onMoveUp = {},
            onMoveDown = {},
        )
    }
}
