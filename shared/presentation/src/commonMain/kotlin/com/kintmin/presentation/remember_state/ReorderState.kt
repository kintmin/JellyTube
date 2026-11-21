package com.kintmin.presentation.remember_state

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * LazyColumn 드래그 재정렬 상태. 아이템 타입에 독립적이며 [idOf]로 아이템을 식별한다.
 *
 * 드래그 핸들의 `detectDragGestures`가 넘겨주는 세로 이동량으로 순서를 계산
 *  1) 드래그로 쌓인 세로 이동량([dragOffset])을 아이템 한 칸 높이([itemHeightPx])로 나눠
 *     "몇 칸 이동했는지"를 구하고, 그만큼 [items]를 실제로 재배치한다.
 *  2) 리스트 가장자리에 닿으면 자동 스크롤로 화면을 따라 밀어준다.
 *
 * 부드러운 재배치 애니메이션(`Modifier.animateItem`), 외부 데이터와의 동기화(`LaunchedEffect`),
 * 드래그 종료 시점의 DB 커밋은 [ReorderState]가 관여하지 않고 호출부에서 배선한다.
 */
class ReorderState<T>(
    private val coroutineScope: CoroutineScope,
    private val idOf: (T) -> Int,
    val listState: LazyListState,
    val items: SnapshotStateList<T>,    // 화면에 표시되는 실제 순서. onDrag가 in-place로 재배치하고 LazyColumn이 이 리스트를 그린다.
    private val scrollZonePx: Float,    // 위/아래 가장자리에서 자동 스크롤이 시작되는 경계 두께(px).
    private val scrollSpeedPx: Float,   // 자동 스크롤 한 프레임 이동량(px).
) {
    // 드래그 시작점 기준 누적된 세로 이동량(px).
    private var dragOffset by mutableFloatStateOf(0f)

    // 최근 손가락 이동 방향. -1 위, +1 아래, 0 미정.
    // (dragOffset 은 스왑 시 이웃 높이만큼 빠지며 부호가 반대로 튈 수 있어 방향 게이트로 쓸 수 없다.)
    private var dragDirection = 0

    // 자동 스크롤 루프. 방향이 바뀔 때만 다시 띄우고, 속도는 루프가 프레임마다 읽는다.
    private var autoScrollJob: Job? = null
    private var autoScrollVelocity = 0f

    // 위치 고정을 요청한 시점의 측정 결과. 다음 측정이 끝나 바뀔 때까지 자동 스크롤을 쉰다.
    private var heldLayoutInfo: LazyListLayoutInfo? = null

    // 뷰포트 위쪽을 덮는 상단 바 높이(px). 덮인 자리에서는 스크롤 존에 닿을 수 없다.
    internal var topInsetPx = 0f

    // 현재 드래그 중인 아이템의 id. null이면 드래그 중이 아니다.
    var draggingItemId by mutableStateOf<Int?>(null)

    private var orderBeforeDrag = emptyList<Int>()

    /** 1px 만 끌고 놓은 것을 커밋하지 않게 한다. */
    val isOrderChanged: Boolean get() = items.map(idOf) != orderBeforeDrag

    /**
     * 잡은 아이템을 **그릴** 거리(px). 호출부가 translationY 로 써서 손가락을 따라오게 한다.
     *
     * 넘어갈 자리가 없는 방향으로는 여기서 잠근다. [dragOffset] 자체는 손가락을 그대로 따라
     * 계속 쌓이므로, 끝에 막힌 뒤 되돌리면 넘어간 만큼 돌아와야 항목이 다시 움직인다 —
     * 누적을 버리면 손가락과 어긋난다.
     */
    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    fun onDragStart(offset: Offset, targetId: Int) {
        // 시작점 대비 누적 이동량을 재기 위해 0에서 시작한다.
        // (핸들 내 터치 y를 넣으면 터치 위치에 따라 이동 감도가 달라진다.)
        dragOffset = 0f
        dragOffsetY = 0f
        dragDirection = 0
        orderBeforeDrag = items.map(idOf)
        draggingItemId = targetId
    }

    fun onDrag(change: PointerInputChange, dragAmount: Offset) {
        change.consume()
        dragOffset += dragAmount.y
        // 손가락이 실제로 움직인 방향만 방향 게이트에 반영(정지 시 마지막 방향 유지).
        if (dragAmount.y < 0f) dragDirection = -1 else if (dragAmount.y > 0f) dragDirection = 1
        settle()
    }

    // 스왑·그릴 거리·자동 스크롤 방향을 지금 배치에 맞춘다. 스크롤 루프도 매 프레임 부른다 —
    // 손가락이 가장자리에 멈춰 있으면 onDrag 가 오지 않는다.
    private fun settle() {
        val info = listState.layoutInfo.visibleItemsInfo
        val currentInfo = info.find { it.key == draggingItemId }
        // 스왑은 배치를 옮긴 만큼 dragOffset 을 되돌려 그려진 위치를 그대로 둔다. 그래서 스왑 전에
        // 잰다 — 스왑 뒤에 옛 배치와 더하면 한 칸 아래로 읽혀 스크롤 존을 벗어난 것으로 판정된다.
        val itemTop = currentInfo?.let { it.offset + dragOffset }
        swapPastNeighbors(info, currentInfo)

        val settledIndex = items.indexOfFirst { idOf(it) == draggingItemId }
        dragOffsetY = clampDragOffsetY(dragOffset, settledIndex, items.lastIndex)

        if (currentInfo == null || itemTop == null) return updateAutoScroll(0f)
        updateAutoScroll(
            autoScrollVelocity(
                itemTop = itemTop,
                itemBottom = itemTop + currentInfo.size,
                viewStart = listState.layoutInfo.viewportStartOffset + topInsetPx,
                viewEnd = listState.layoutInfo.viewportEndOffset.toFloat(),
                scrollZone = scrollZonePx,
                dragDirection = dragDirection,
                canMoveUp = settledIndex > 0,
                canMoveDown = settledIndex in 0 until items.lastIndex,
                canScrollBackward = listState.canScrollBackward,
                canScrollForward = listState.canScrollForward,
            )
        )
    }

    // 빠르게 끌면 한 이벤트에 여러 칸을 지나므로 절반을 넘긴 이웃은 모두 바꾼다.
    // 스왑마다 |dragOffset| 이 줄어 반드시 끝난다.
    private fun swapPastNeighbors(info: List<LazyListItemInfo>, currentInfo: LazyListItemInfo?) {
        var index = items.indexOfFirst { idOf(it) == draggingItemId }
        if (index == -1) return
        val firstVisible = listState.firstVisibleItemIndex
        var touchesFirstVisible = currentInfo?.index == firstVisible
        var swapped = false
        while (dragOffset != 0f) {
            val step = if (dragOffset > 0f) 1 else -1
            val neighbor = items.getOrNull(index + step) ?: break
            // 이웃이 화면 밖이면 멈춘다. 자동 스크롤이 화면을 밀어 이웃을 드러낸다.
            val neighborInfo = info.find { it.key == idOf(neighbor) } ?: break
            if (abs(dragOffset) <= neighborInfo.size / 2f) break
            if (neighborInfo.index == firstVisible) touchesFirstVisible = true
            items.add(index + step, items.removeAt(index))
            dragOffset -= step * neighborInfo.size
            index += step
            swapped = true
        }
        if (swapped && touchesFirstVisible) holdScrollPosition()
    }

    // LazyList 는 첫 보이는 항목의 key 를 따라 스크롤 위치를 옮긴다. 그 항목이 스왑에 끼면
    // 목록이 한 칸 튀어 잡은 항목이 화면 밖으로 밀려나고, 컴포지션에서 빠지며 끌기가 취소된다.
    private fun holdScrollPosition() {
        listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        heldLayoutInfo = listState.layoutInfo
    }

    private fun updateAutoScroll(velocity: Float) {
        val isSameDirection = sign(velocity) == sign(autoScrollVelocity)
        autoScrollVelocity = velocity
        if (isSameDirection) return
        autoScrollJob?.cancel()
        if (velocity == 0f) return
        autoScrollJob = coroutineScope.launch {
            while (isActive) {
                // 고정 지연으로 돌면 화면 갱신과 어긋나 한 프레임에 두 번 또는 0 번 스크롤된다.
                withFrameNanos { }
                // 위치 고정은 다음 측정에서 적용된다. 그 전에 스크롤하면 측정이 요청 위치로 덮어써
                // 이번 스크롤이 사라지고, 이미 보정한 dragOffset 만큼 항목이 덜컥 튄다.
                if (heldLayoutInfo === listState.layoutInfo) continue
                heldLayoutInfo = null
                // 실제로 스크롤된 양만큼만 dragOffset 을 보정한다.
                // 끝에 닿아 스크롤이 0이면 보정도 0 → 헛누적으로 인한 폭주를 막는다.
                dragOffset += listState.scrollBy(autoScrollVelocity * scrollSpeedPx)
                settle()
            }
        }
    }

    fun onDragEnd() {
        draggingItemId = null
        updateAutoScroll(0f)
    }

    // 드래그가 끝난 시점의 최종 인덱스. 호출부가 이 값으로 커밋 여부/순서를 판단한다.
    fun getDraggingItemIndex(): Int? {
        return items.indexOfFirst { idOf(it) == draggingItemId }.takeIf { it != -1 }
    }
}

/** 자동 스크롤 속도. 부호가 방향(- 위, + 아래)이고 크기는 존에 들어간 깊이다. */
internal fun autoScrollVelocity(
    itemTop: Float,
    itemBottom: Float,
    viewStart: Float,
    viewEnd: Float,
    scrollZone: Float,
    dragDirection: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
): Float = when {
    canMoveUp && dragDirection < 0 && canScrollBackward && itemTop < viewStart + scrollZone ->
        -depthFraction(viewStart + scrollZone - itemTop, scrollZone)
    canMoveDown && dragDirection > 0 && canScrollForward && itemBottom > viewEnd - scrollZone ->
        depthFraction(itemBottom - (viewEnd - scrollZone), scrollZone)
    else -> 0f
}

private fun depthFraction(depth: Float, scrollZone: Float): Float =
    (depth / scrollZone).coerceIn(MIN_AUTO_SCROLL_FRACTION, 1f)

private const val MIN_AUTO_SCROLL_FRACTION = 0.25f

internal fun clampDragOffsetY(offset: Float, index: Int, lastIndex: Int): Float {
    var clamped = offset
    if (index <= 0) clamped = clamped.coerceAtLeast(0f)
    if (index >= lastIndex) clamped = clamped.coerceAtMost(0f)
    return clamped
}

internal fun <T> mergeKeepingOrder(current: List<T>, incoming: List<T>, idOf: (T) -> Int): List<T> {
    val incomingById = incoming.associateBy(idOf)
    val kept = current.mapNotNull { incomingById[idOf(it)] }
    val keptIds = kept.mapTo(mutableSetOf(), idOf)
    return kept + incoming.filterNot { idOf(it) in keptIds }
}

@Composable
fun <T> rememberReorderState(
    listState: LazyListState = rememberLazyListState(),
    dataList: List<T>,
    idOf: (T) -> Int,
    scrollZone: Dp = 80.dp,
    scrollSpeed: Dp = 12.dp,
    topInset: Dp = 0.dp,
): ReorderState<T> {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<T>().also { it.addAll(dataList) } }

    val state = remember {
        ReorderState(
            listState = listState,
            coroutineScope = coroutineScope,
            idOf = idOf,
            items = items,
            scrollZonePx = with(density) { scrollZone.toPx() },
            scrollSpeedPx = with(density) { scrollSpeed.toPx() },
        )
    }
    state.topInsetPx = with(density) { topInset.toPx() }

    LaunchedEffect(dataList) {
        // 끄는 중이라고 건너뛰면 그 방출은 다시 오지 않는다. 키가 되돌아오지 않아서다.
        val next = if (state.draggingItemId == null) {
            dataList
        } else {
            mergeKeepingOrder(state.items, dataList, idOf)
        }
        state.items.clear()
        state.items.addAll(next)
    }

    return state
}
