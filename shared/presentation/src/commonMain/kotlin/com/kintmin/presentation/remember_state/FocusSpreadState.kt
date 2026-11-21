package com.kintmin.presentation.remember_state

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlin.math.abs
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/** [com.kintmin.presentation.animation.FocusSpreadOverlay] 를 [highlightedId] 의 행에 [progress] 로 그린다. */
@Stable
class FocusSpreadState {
    var highlightedId by mutableStateOf<Int?>(null)
        internal set
    var progress by mutableFloatStateOf(0f)
        internal set
}

/**
 * [focusId] 의 행을 목록 가운데로 끌어와 한 번 퍼지는 강조를 그린다.
 * 같은 id 는 화면을 다시 만들어도 되풀이하지 않는다. [indexOffset] 은 목록 앞의 머리 항목 수다.
 */
@Composable
fun rememberFocusSpreadState(
    listState: LazyListState,
    focusId: Int?,
    ids: List<Int>,
    indexOffset: Int = 0,
): FocusSpreadState {
    val state = remember { FocusSpreadState() }
    var consumedFocusId by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(focusId, ids) {
        if (focusId == null || consumedFocusId == focusId) return@LaunchedEffect
        val indexInList = ids.indexOf(focusId)
        if (indexInList < 0) return@LaunchedEffect

        val itemIndex = indexInList + indexOffset
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == itemIndex }) {
            listState.scrollToItem(itemIndex)
        }
        val itemInfo = snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == itemIndex }
        }.filterNotNull().first()

        val layoutInfo = listState.layoutInfo
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val deltaToCenter = (itemInfo.offset + itemInfo.size / 2 - viewportCenter).toFloat()
        if (abs(deltaToCenter) > 1f) {
            listState.scrollBy(deltaToCenter)
        }

        state.highlightedId = focusId
        state.progress = 0f
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        ) { value, _ ->
            state.progress = value
        }
        state.progress = 0f
        state.highlightedId = null
        consumedFocusId = focusId
    }
    return state
}
