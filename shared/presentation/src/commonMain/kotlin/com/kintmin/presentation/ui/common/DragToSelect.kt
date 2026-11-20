package com.kintmin.presentation.ui.common

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 끌기를 시작한 항목([anchorId])의 상태를 뒤집는 쪽으로, 시작점부터 [currentId] 까지를 한꺼번에 맞춘다.
 * 범위에서 다시 빠진 항목은 [initialCheckedIds] 대로 돌아간다.
 */
fun dragSelectedIds(
    orderedIds: List<Int>,
    initialCheckedIds: Set<Int>,
    anchorId: Int,
    currentId: Int,
): Set<Int> {
    val anchorIndex = orderedIds.indexOf(anchorId)
    val currentIndex = orderedIds.indexOf(currentId)
    if (anchorIndex < 0 || currentIndex < 0) return initialCheckedIds

    val range = orderedIds.subList(minOf(anchorIndex, currentIndex), maxOf(anchorIndex, currentIndex) + 1)
    val isSelecting = anchorId !in initialCheckedIds
    return if (isSelecting) initialCheckedIds + range else initialCheckedIds - range.toSet()
}

/**
 * 길게 누른 뒤 끌어서 여러 항목을 한 번에 고른다. LazyColumn 에 붙이고, 항목 key 는 [selectableIds] 의 id 여야 한다.
 *
 * 자식이 끌기를 먼저 먹으면(순서 변경 핸들) 길게 누르기가 취소되어 이 제스처는 시작되지 않는다.
 */
@Composable
fun Modifier.dragToSelect(
    listState: LazyListState,
    selectableIds: List<Int>,
    checkedIds: Set<Int>,
    onCheckedIdsChange: (Set<Int>) -> Unit,
): Modifier {
    val haptic = LocalHapticFeedback.current
    val edgePx = with(LocalDensity.current) { AUTO_SCROLL_EDGE.toPx() }
    // pointerInput 은 키만 비교해 람다가 첫 구성 시점의 값에 묶인다.
    val currentSelectableIds by rememberUpdatedState(selectableIds)
    val currentCheckedIds by rememberUpdatedState(checkedIds)
    val currentOnCheckedIdsChange by rememberUpdatedState(onCheckedIdsChange)
    val session = remember { DragSelectSession() }
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }

    fun idAt(y: Float): Int? = listState.layoutInfo.visibleItemsInfo
        .firstOrNull { y.toInt() in it.offset until it.offset + it.size }
        ?.key
        ?.let { it as? Int }
        ?.takeIf { it in currentSelectableIds }

    fun select(currentId: Int) {
        val anchorId = session.anchorId ?: return
        currentOnCheckedIdsChange(
            dragSelectedIds(currentSelectableIds, session.initialCheckedIds, anchorId, currentId)
        )
    }

    // 손가락이 가장자리에 멈춰 있으면 onDrag 가 오지 않으므로, 스크롤할 때마다 손가락 밑 항목을 다시 고른다.
    LaunchedEffect(autoScrollSpeed) {
        if (autoScrollSpeed == 0f) return@LaunchedEffect
        while (isActive) {
            listState.scrollBy(autoScrollSpeed)
            idAt(session.pointerY)?.let { select(it) }
            delay(AUTO_SCROLL_FRAME_MILLIS)
        }
    }

    return pointerInput(listState) {
        fun stop() {
            session.anchorId = null
            autoScrollSpeed = 0f
        }

        detectDragGesturesAfterLongPress(
            onDragStart = start@{ offset ->
                val anchorId = idAt(offset.y) ?: return@start
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                session.anchorId = anchorId
                session.initialCheckedIds = currentCheckedIds
                session.pointerY = offset.y
                select(anchorId)
            },
            onDrag = drag@{ change, _ ->
                if (session.anchorId == null) return@drag
                change.consume()
                val y = change.position.y
                session.pointerY = y
                idAt(y)?.let { select(it) }
                autoScrollSpeed = when {
                    y < edgePx -> -(edgePx - y) / AUTO_SCROLL_DIVISOR
                    y > size.height - edgePx -> (y - (size.height - edgePx)) / AUTO_SCROLL_DIVISOR
                    else -> 0f
                }
            },
            onDragEnd = { stop() },
            onDragCancel = { stop() },
        )
    }
}

private class DragSelectSession {
    var anchorId: Int? = null
    var initialCheckedIds: Set<Int> = emptySet()
    var pointerY: Float = 0f
}

private val AUTO_SCROLL_EDGE = 64.dp
// 가장자리에 깊이 들어갈수록 빨라진다. 끝에 닿으면 프레임당 약 edge/4 px.
private const val AUTO_SCROLL_DIVISOR = 4f
private const val AUTO_SCROLL_FRAME_MILLIS = 16L
