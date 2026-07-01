package com.kintmin.presentation.remember_state

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * LazyColumn 드래그 재정렬 상태. 아이템 타입에 독립적이며 [idOf]로 아이템을 식별한다.
 *
 * 드래그 핸들의 `detectDragGestures`가 넘겨주는 세로 이동량으로 순서를 계산
 *  1) 드래그로 쌓인 세로 이동량([dragOffset])을 아이템 한 칸 높이([itemHeightPx])로 나눠
 *     "몇 칸 이동했는지"를 구하고, 그만큼 [items]를 실제로 재배치한다.
 *  2) 리스트 가장자리에 닿으면 자동 스크롤(overscroll)로 화면을 따라 밀어준다.
 *
 * 부드러운 재배치 애니메이션(`Modifier.animateItem`), 외부 데이터와의 동기화(`LaunchedEffect`),
 * 드래그 종료 시점의 DB 커밋은 [ReorderState]가 관여하지 않고 호출부에서 배선한다.
 */
class ReorderState<T>(
    private val coroutineScope: CoroutineScope,
    private val idOf: (T) -> Int,
    val listState: LazyListState,
    val items: SnapshotStateList<T>,    // 화면에 표시되는 실제 순서. onDrag가 in-place로 재배치하고 LazyColumn이 이 리스트를 그린다.
    initializeItemHeightPx: Float,
    private val scrollZonePx: Float,    // 위/아래 가장자리에서 자동 스크롤이 시작되는 경계 두께(px).
    private val scrollSpeedPx: Float,   // 자동 스크롤 1회 이동량(px).
) {
    // 아이템 한 칸의 높이(px). 몇 칸을 넘었는지 계산하는 기준이 된다.
    private var itemHeightPx by mutableFloatStateOf(initializeItemHeightPx)

    // 드래그 시작점 기준 누적된 세로 이동량(px).
    private var dragOffset by mutableFloatStateOf(0f)
    private var draggingItemOffset by mutableIntStateOf(0)

    // 자동 스크롤 코루틴. 중복 실행을 막기 위해 진행 중이면 재시작하지 않는다.
    private var overscrollJob by mutableStateOf<Job?>(null)

    // 현재 드래그 중인 아이템의 id. null이면 드래그 중이 아니다.
    var draggingItemId by mutableStateOf<Int?>(null)

    fun onDragStart(offset: Offset, targetId: Int) {
        // 시작점 대비 누적 이동량을 재기 위해 0에서 시작한다.
        // (핸들 내 터치 y를 넣으면 터치 위치에 따라 이동 감도가 달라진다.)
        dragOffset = 0f
        draggingItemId = targetId

        listState.layoutInfo.visibleItemsInfo.find { it.key == targetId }?.let { info ->
            draggingItemOffset = info.offset
        }
    }

    fun onDrag(change: PointerInputChange, dragAmount: Offset) {
        change.consume()
        dragOffset += dragAmount.y

        // 누적 이동량을 칸 높이로 나눠 반올림 → 위/아래 대칭으로 반 칸(±0.5)을 넘으면 그 방향으로 이동한다.
        val moved = (dragOffset / itemHeightPx).roundToInt()

        val fromIndex = items.indexOfFirst { idOf(it) == draggingItemId }
        if (fromIndex == -1) return

        val toIndex = (fromIndex + moved).coerceIn(0, items.lastIndex)

        if (fromIndex != toIndex) {
            // 실제로 리스트에서 아이템을 옮기고(remove 후 재삽입 = move),
            // 소비한 칸 수만큼 dragOffset을 덜어내 남은 이동량만 유지한다(연속 드래그 오차 방지).
            items.add(toIndex, items.removeAt(fromIndex))
            dragOffset -= (moved * itemHeightPx)
        }

        // 오토 스크롤: 드래그 아이템이 뷰포트 위/아래 경계(scrollZone) 안에 들어오면 그 방향으로 스크롤한다.
        val scrollZone = scrollZonePx
        val scrollSpeed = scrollSpeedPx

        val currentItem = listState.layoutInfo.visibleItemsInfo.find { it.key == draggingItemId }

        val canScrollDown = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.let { lastItem ->
            val bottomReached = lastItem.offset + lastItem.size <= listState.layoutInfo.viewportEndOffset
            !(bottomReached)
        } ?: false
        // 위로 더 스크롤할 여지가 있는지. 이 가드가 없으면 최상단 아이템에서 실제 스크롤은 0인데
        // dragOffset -= scrollSpeed 만 계속 실행되어, 상단 아이템이 위로 편향돼 오작동한다.
        val canScrollUp = listState.canScrollBackward

        if (currentItem != null) {
            val itemTop = currentItem.offset
            val itemBottom = itemTop + currentItem.size

            val viewStart = listState.layoutInfo.viewportStartOffset
            val viewEnd = listState.layoutInfo.viewportEndOffset

            when {
                // 위쪽 경계 안이고 더 스크롤할 여지가 있으면 → 위로 스크롤. dragOffset도 같이 보정한다.
                itemTop < viewStart + scrollZone && canScrollUp -> {
                    if (overscrollJob?.isActive == true) return
                    overscrollJob = coroutineScope.launch {
                        listState.scrollBy(-scrollSpeed)
                        dragOffset -= scrollSpeed
                    }
                }

                // 아래쪽 경계 안이고 더 스크롤할 여지가 있으면 → 아래로 스크롤.
                itemBottom > viewEnd - scrollZone && canScrollDown -> {
                    if (overscrollJob?.isActive == true) return
                    overscrollJob = coroutineScope.launch {
                        listState.scrollBy(scrollSpeed)
                        dragOffset += scrollSpeed
                    }
                }

                else -> overscrollJob?.cancel()
            }
        }
    }

    fun onDragEnd() {
        draggingItemId = null
        overscrollJob?.cancel()
    }

    // 드래그가 끝난 시점의 최종 인덱스. 호출부가 이 값으로 커밋 여부/순서를 판단한다.
    fun getDraggingItemIndex(): Int? {
        return items.indexOfFirst { idOf(it) == draggingItemId }.takeIf { it != -1 }
    }
}

@Composable
fun <T> rememberReorderState(
    listState: LazyListState = rememberLazyListState(),
    dataList: List<T>,
    idOf: (T) -> Int,
    initializeItemHeightPx: Dp,
    scrollZone: Dp = 80.dp,
    scrollSpeed: Dp = 12.dp,
): ReorderState<T> {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<T>().also { it.addAll(dataList) } }

    return remember {
        ReorderState(
            listState = listState,
            coroutineScope = coroutineScope,
            idOf = idOf,
            items = items,
            initializeItemHeightPx = with(density) { initializeItemHeightPx.toPx() },
            scrollZonePx = with(density) { scrollZone.toPx() },
            scrollSpeedPx = with(density) { scrollSpeed.toPx() },
        )
    }
}
