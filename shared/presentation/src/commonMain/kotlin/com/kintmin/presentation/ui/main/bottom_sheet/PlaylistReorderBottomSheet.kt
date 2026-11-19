package com.kintmin.presentation.ui.main.bottom_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60
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
        // 드래그 중에 상위 목록이 바뀌면(다운로드 완료로 곡 수가 바뀌는 등) 손에 쥔 배치가 재설정된다.
        if (reorderState.draggingItemId != null) return@LaunchedEffect
        reorderState.items.clear()
        reorderState.items.addAll(playlists)
    }

    JellyTubeBottomSheet(
        onDismissRequest = onDismissRequest,
        // 드래그 중에는 시트 자체 드래그를 잠가 reorder 제스처와의 충돌을 막는다.
        sheetGesturesEnabled = reorderState.draggingItemId == null,
    ) {
        // 머리말과 버튼을 Column 으로 감싸면 LazyColumn 이 높이 제약을 잃는다. 같은 리스트에 넣는다.
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
        ) {
            item(key = "header") {
                // 범위는 제목이 말한다. 이 시트는 내 플레이리스트 머리말에서만 열리고
                // 라이브러리는 목록에 아예 없으므로 따로 설명할 것이 없다.
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    text = "내 플레이리스트 순서",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            itemsIndexed(
                items = reorderState.items,
                key = { _, item -> item.id },
            ) { index, item ->
                PlaylistReorderItemView(
                    modifier = Modifier.animateItem(),
                    data = item,
                    draggingItemId = reorderState.draggingItemId,
                    canMoveUp = index > 0,
                    canMoveDown = index < reorderState.items.lastIndex,
                    onMoveUp = { reorderState.items.add(index - 1, reorderState.items.removeAt(index)) },
                    onMoveDown = { reorderState.items.add(index + 1, reorderState.items.removeAt(index)) },
                    onDragStart = reorderState::onDragStart,
                    onDrag = reorderState::onDrag,
                    onDragEnd = reorderState::onDragEnd,
                )
            }
            item(key = "actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        onClick = onDismissRequest,
                    ) {
                        Text(text = "취소", fontSize = 14.sp, lineHeight = 14.sp)
                    }
                    Button(
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        onClick = {
                            onReorder(reorderState.items.map { it.id })
                            onDismissRequest()
                        },
                    ) {
                        Text(text = "완료", fontSize = 14.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistReorderItemView(
    modifier: Modifier,
    data: PlaylistItemUiState,
    draggingItemId: Int?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
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
            modifier = Modifier
                .size(48.dp)
                .padding(12.dp)
                .pointerInput(data.id, data.sequence) {
                    detectDragGestures(
                        onDragStart = { onDragStart(it, data.id) },
                        onDrag = { change, dragAmount -> onDrag(change, dragAmount) },
                        onDragEnd = { onDragEnd() },
                        // 제스처가 취소되면 draggingItemId 가 남아 시트를 끌어 닫을 수 없게 된다.
                        onDragCancel = { onDragEnd() },
                    )
                },
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
            modifier = Modifier,
            data = PlaylistItemUiState.getMock(id = 1),
            draggingItemId = null,
            canMoveUp = false,
            canMoveDown = true,
            onMoveUp = {},
            onMoveDown = {},
            onDragStart = { _, _ -> },
            onDrag = { _, _ -> },
            onDragEnd = {},
        )
    }
}
