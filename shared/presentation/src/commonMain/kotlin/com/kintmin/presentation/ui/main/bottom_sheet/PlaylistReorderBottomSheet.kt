package com.kintmin.presentation.ui.main.bottom_sheet

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray20
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.ui.common.JellyTubeBottomSheet
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState
import com.kintmin.presentation.remember_state.rememberReorderState

private val ITEM_HEIGHT = 56.dp
private val DRAG_ELEVATION = 8.dp
private val DRAG_INSET = 12.dp
private val DRAG_CORNER = 12.dp

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
    // 드래그 시작 시점의 순서. 끝났을 때 실제로 바뀐 경우에만 쓴다 — 1px 만 끌어도 전체
    // sequence 를 다시 쓰던 것을 막는다.
    var orderBeforeDrag by remember { mutableStateOf(emptyList<Int>()) }

    fun commitOrder() = onReorder(reorderState.items.map { it.id })

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
                PlaylistReorderItemView(
                    modifier = Modifier.animateItem(
                        // 잡은 행의 위치는 translationY 가 직접 몬다. 스왑 때 배치까지 애니메이션하면
                        // 즉시 빠지는 dragOffset 과 더해져 한 번 튀었다 돌아온다. 그 행만 끈다.
                        placementSpec = if (item.id == reorderState.draggingItemId) {
                            null
                        } else {
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                                visibilityThreshold = IntOffset.VisibilityThreshold,
                            )
                        },
                    ),
                    data = item,
                    draggingItemId = reorderState.draggingItemId,
                    dragOffsetProvider = { reorderState.dragOffsetY },
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
                    onDragStart = { offset, id ->
                        orderBeforeDrag = reorderState.items.map { it.id }
                        reorderState.onDragStart(offset, id)
                    },
                    onDrag = reorderState::onDrag,
                    onDragEnd = {
                        reorderState.onDragEnd()
                        if (reorderState.items.map { it.id } != orderBeforeDrag) commitOrder()
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
    dragOffsetProvider: () -> Float,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragStart: (Offset, Int) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val isDragging = data.id == draggingItemId

    // 놓은 뒤 제자리로 내려앉는 동안의 값. 끄는 동안에는 쓰지 않는다.
    val settle = remember { Animatable(0f) }
    // 잡은 순간부터 착지가 끝날 때까지 참. 프레임마다 바뀌지 않아 재구성을 부르지 않는다.
    var isLifted by remember { mutableStateOf(false) }

    LaunchedEffect(isDragging) {
        if (isDragging) {
            isLifted = true
        } else if (isLifted) {
            // 놓는 시점의 offset 에서 시작해 0 으로 내려앉는다.
            settle.snapTo(dragOffsetProvider())
            settle.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            isLifted = false
        }
    }

    val elevation = animateDpAsState(
        targetValue = if (isDragging) DRAG_ELEVATION else 0.dp,
        label = "reorderDragElevation",
    )
    // 집었다 놓는 느낌은 이동 거리가 아니라 들림이 꺼지는 데서 온다. 스왑이 이웃 높이를
    // 빼가므로 놓는 순간 항목은 이미 최종 자리 근처라, 거리에 기대면 아무것도 느껴지지 않는다.
    //
    // 폭을 꽉 채운 띠에서는 scale 도 그림자도 신호가 못 된다 — 56dp 의 2% 는 1픽셀이고,
    // 다크 테마에서 검은 그림자는 거의 검은 배경 위에 그려진다. 띠에서 떨어져 나오게 해야
    // 들린 것으로 읽힌다.
    val inset = animateDpAsState(
        targetValue = if (isDragging) DRAG_INSET else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "reorderDragInset",
    )
    val corner = animateDpAsState(
        targetValue = if (isDragging) DRAG_CORNER else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "reorderDragCorner",
    )
    // 들어 올린 것은 같은 색 계열에서 한 단계 밝게 한다. 색상을 바꾸면 들림이 아니라
    // 다른 종류라는 뜻이 된다. gray20 은 surface 에 onSurface 16% 를 올린 값에 가깝다.
    val background by animateColorAsState(
        targetValue = if (isDragging) gray20 else MaterialTheme.colorScheme.surface,
        label = "reorderDragBackground",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ITEM_HEIGHT)
            .zIndex(if (isLifted) 1f else 0f)
            .padding(horizontal = inset.value)
            .graphicsLayer {
                // 프레임마다 바뀌는 값은 여기서만 읽는다. Composable 범위에서 읽으면 드래그
                // 한 프레임마다 모든 행이 재구성돼 깜박인다.
                translationY = if (isDragging) dragOffsetProvider() else settle.value
                shadowElevation = elevation.value.toPx()
                shape = RoundedCornerShape(corner.value)
                clip = true
            }
            .background(background)
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
            dragOffsetProvider = { 0f },
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
