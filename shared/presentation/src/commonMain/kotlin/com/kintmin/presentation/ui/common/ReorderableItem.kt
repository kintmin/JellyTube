package com.kintmin.presentation.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kintmin.presentation.remember_state.ReorderState
import com.kintmin.presentation.remember_state.rememberReorderState
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted

private val DRAG_ELEVATION = 8.dp
private val DRAG_INSET = 12.dp
private val DRAG_CORNER = 12.dp
private const val DRAG_STATE_LAYER_ALPHA = 0.16f

/**
 * [ReorderState] 로 끌어 옮기는 LazyColumn 항목. 들림 표현과 제스처 배선을 담는다.
 *
 * [content] 는 끌기를 받을 자리에 [dragHandleModifier] 를 붙인다 — 그 자리만 제스처를 먹고
 * 나머지는 평소처럼 눌린다. 순서를 저장하는 것은 호출부 몫이다([onDragEnd]) — 플레이리스트는
 * 정렬된 id 전체를, 트랙은 옮긴 한 건을 보내기 때문이다.
 *
 * [longPressToStart] 면 핸들 없이 행 전체에 붙일 수 있다. 세로 드래그를 즉시 가로채면
 * LazyColumn 스크롤이 죽으므로, 이 경우에만 롱프레스를 기다린 뒤 끌기를 받는다.
 */
@Composable
fun <T> LazyItemScope.ReorderableItem(
    state: ReorderState<T>,
    id: Int,
    modifier: Modifier = Modifier,
    longPressToStart: Boolean = false,
    onDragEnd: () -> Unit,
    content: @Composable BoxScope.(dragHandleModifier: Modifier) -> Unit,
) {
    val isDragging = id == state.draggingItemId

    // 놓은 뒤 제자리로 내려앉는 동안의 값. 끄는 동안에는 쓰지 않는다.
    val settle = remember { Animatable(0f) }
    // 잡은 순간부터 착지가 끝날 때까지 참. 프레임마다 바뀌지 않아 재구성을 부르지 않는다.
    var isLifted by remember { mutableStateOf(false) }

    LaunchedEffect(isDragging) {
        if (isDragging) {
            isLifted = true
        } else if (isLifted) {
            settle.snapTo(state.dragOffsetY)
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

    // 스왑이 이웃 높이를 빼가므로 놓는 순간 항목은 이미 최종 자리 근처다 — 이동 거리로는
    // 들림이 읽히지 않는다. 폭을 꽉 채운 띠라 scale 도 그림자도 신호가 못 된다(다크 테마에서
    // 검은 그림자는 거의 검은 배경 위에 그려진다). 띠에서 떼어내는 것이 유일하게 보이는 신호다.
    val elevation = animateDpAsState(
        targetValue = if (isDragging) DRAG_ELEVATION else 0.dp,
        label = "reorderDragElevation",
    )
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
    // 드래그 상태 레이어는 내용 **위에** 그린다 — 뒤에 깔면 호출부의 행이 불투명한
    // 배경을 가질 때(Card) 통째로 가려져 끄는 동안 아무 색도 변하지 않는다.
    val stateLayerColor = MaterialTheme.colorScheme.onSurface
    // 행이 바탕을 칠하지 않으면 들린 동안 아래 행이 비치고 그림자가 선처럼 드러난다.
    val liftedSurfaceColor = MaterialTheme.colorScheme.surface
    val stateLayerAlpha = animateFloatAsState(
        targetValue = if (isDragging) DRAG_STATE_LAYER_ALPHA else 0f,
        label = "reorderDragStateLayer",
    )

    // pointerInput 은 키만 비교하고 람다는 비교하지 않는다. 키가 그대로면 처음 구성 때 잡힌
    // onDragEnd 가 계속 돌아, 호출부가 그 안에서 읽는 값이 첫 구성 시점에 묶인다.
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val haptic = LocalHapticFeedback.current
    val dragHandleModifier = Modifier.pointerInput(id, longPressToStart) {
        // 감지기는 끌기를 시작하지 않았어도 코루틴이 취소되면 onDragCancel 을 부른다. 자동 스크롤로
        // 다른 행이 화면 밖으로 빠질 때마다 그 행의 취소가 남의 끌기를 끝내지 않게, 이 행이 시작한
        // 끌기만 끝낸다.
        var isDraggingHere = false

        fun finish() {
            if (!isDraggingHere) return
            isDraggingHere = false
            // 호출부를 먼저 부른다 — draggingItemId 가 아직 살아 있어야 옮긴 항목을 찾을 수 있다.
            currentOnDragEnd()
            state.onDragEnd()
        }

        fun start(offset: Offset) {
            isDraggingHere = true
            // 핸들이 없으면 잡혔다는 것을 알리는 신호가 햅틱뿐이다.
            if (longPressToStart) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            state.onDragStart(offset, id)
        }

        val onDrag: (PointerInputChange, Offset) -> Unit = { change, dragAmount ->
            state.onDrag(change, dragAmount)
        }

        if (longPressToStart) {
            detectDragGesturesAfterLongPress(
                onDragStart = { start(it) },
                onDrag = onDrag,
                onDragEnd = { finish() },
                // 제스처가 취소되면 draggingItemId 가 남아 목록이 끌기에 잠긴 채로 끝난다.
                onDragCancel = { finish() },
            )
        } else {
            detectDragGestures(
                onDragStart = { start(it) },
                onDrag = onDrag,
                onDragEnd = { finish() },
                onDragCancel = { finish() },
            )
        }
    }

    Box(
        modifier = modifier
            .animateItem(
                // 잡은 행의 위치는 translationY 가 직접 몬다. 스왑 때 배치까지 애니메이션하면
                // 즉시 빠지는 dragOffset 과 더해져 한 번 튀었다 돌아온다. 그 행만 끈다.
                // 비켜 주는 다른 행들의 애니메이션은 그대로 둔다.
                placementSpec = if (isDragging) {
                    null
                } else {
                    spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    )
                },
            )
            .zIndex(if (isLifted) 1f else 0f)
            .padding(horizontal = inset.value)
            .graphicsLayer {
                // 프레임마다 바뀌는 값은 여기서만 읽는다. Composable 범위에서 읽으면 드래그
                // 한 프레임마다 모든 행이 재구성돼 깜박인다.
                translationY = if (isDragging) state.dragOffsetY else settle.value
                shadowElevation = elevation.value.toPx()
                shape = RoundedCornerShape(corner.value)
                clip = true
            }
            .background(if (isLifted) liftedSurfaceColor else Color.Transparent)
            .drawWithContent {
                drawContent()
                // 프레임마다 바뀌는 값은 여기서만 읽는다. 바깥에서 읽으면 끄는 한 프레임마다
                // 행이 재구성된다.
                val alpha = stateLayerAlpha.value
                if (alpha > 0f) drawRect(color = stateLayerColor, alpha = alpha)
            },
    ) {
        content(dragHandleModifier)
    }
}

@Preview
@Composable
private fun ReorderableItemPreview() {
    JellyTubeTheme {
        val items = listOf(1, 2, 3)
        val state = rememberReorderState(dataList = items, idOf = { it })
        LazyColumn {
            items(state.items.size) { index ->
                val item = state.items[index]
                ReorderableItem(
                    state = state,
                    id = item,
                    modifier = Modifier.height(56.dp),
                    onDragEnd = {},
                ) { dragHandleModifier ->
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(text = "항목 $item")
                        Icon(
                            modifier = dragHandleModifier.align(Alignment.CenterEnd),
                            imageVector = Icons.Rounded.Reorder,
                            contentDescription = "끌어서 순서 변경",
                            tint = contentMuted,
                        )
                    }
                }
            }
        }
    }
}
