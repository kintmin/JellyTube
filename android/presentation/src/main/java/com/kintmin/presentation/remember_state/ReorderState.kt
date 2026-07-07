package com.kintmin.presentation.remember_state

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
    private val scrollZonePx: Float,    // 위/아래 가장자리에서 자동 스크롤이 시작되는 경계 두께(px).
    private val scrollSpeedPx: Float,   // 자동 스크롤 1회 이동량(px).
) {
    // 드래그 시작점 기준 누적된 세로 이동량(px).
    private var dragOffset by mutableFloatStateOf(0f)

    // 최근 손가락 이동 방향. -1 위, +1 아래, 0 미정.
    // (dragOffset 은 스왑 시 이웃 높이만큼 빠지며 부호가 반대로 튈 수 있어 방향 게이트로 쓸 수 없다.)
    private var dragDirection = 0

    // 자동 스크롤 코루틴. 중복 실행을 막기 위해 진행 중이면 재시작하지 않는다.
    private var overscrollJob by mutableStateOf<Job?>(null)

    // 현재 드래그 중인 아이템의 id. null이면 드래그 중이 아니다.
    var draggingItemId by mutableStateOf<Int?>(null)

    fun onDragStart(offset: Offset, targetId: Int) {
        // 시작점 대비 누적 이동량을 재기 위해 0에서 시작한다.
        // (핸들 내 터치 y를 넣으면 터치 위치에 따라 이동 감도가 달라진다.)
        dragOffset = 0f
        dragDirection = 0
        draggingItemId = targetId
    }

    fun onDrag(change: PointerInputChange, dragAmount: Offset) {
        change.consume()
        dragOffset += dragAmount.y
        // 손가락이 실제로 움직인 방향만 방향 게이트에 반영(정지 시 마지막 방향 유지).
        if (dragAmount.y < 0f) dragDirection = -1 else if (dragAmount.y > 0f) dragDirection = 1

        // 고정 높이 가정 없이, 실제 이웃 아이템의 렌더 높이(visibleItemsInfo.size)를 기준으로
        // 한 프레임에 최대 한 칸씩만 스왑한다. 동적(가변) 높이 아이템에서도 정확하다.
        val info = listState.layoutInfo.visibleItemsInfo
        val fromIndex = items.indexOfFirst { idOf(it) == draggingItemId }
        if (fromIndex != -1) {
            if (dragOffset > 0f) {
                // 아래로: 바로 아래 이웃의 절반을 넘으면 그 이웃과 자리를 바꾸고, 이웃 높이만큼 소비한다.
                val next = items.getOrNull(fromIndex + 1)
                val nextInfo = next?.let { n -> info.find { it.key == idOf(n) } }
                if (nextInfo != null && dragOffset > nextInfo.size / 2f) {
                    items.add(fromIndex + 1, items.removeAt(fromIndex))
                    dragOffset -= nextInfo.size
                }
            } else if (dragOffset < 0f) {
                // 위로: 바로 위 이웃의 절반을 넘으면 스왑.
                val prev = items.getOrNull(fromIndex - 1)
                val prevInfo = prev?.let { p -> info.find { it.key == idOf(p) } }
                if (prevInfo != null && -dragOffset > prevInfo.size / 2f) {
                    items.add(fromIndex - 1, items.removeAt(fromIndex))
                    dragOffset += prevInfo.size
                }
            }
        }
        // 이웃이 화면 밖이면 스왑을 건너뛰되, 아래 오토스크롤이 화면을 밀어 이웃을 노출시킨다.

        // 오토 스크롤: 드래그 아이템이 뷰포트 위/아래 경계(scrollZone) 안에 들어오면 그 방향으로 스크롤한다.
        val currentItem = info.find { it.key == draggingItemId }

        if (currentItem != null) {
            val viewStart = listState.layoutInfo.viewportStartOffset
            val viewEnd = listState.layoutInfo.viewportEndOffset
            // 드래그 누적량을 반영한 실제 시각 위치로 경계를 판정한다.
            val itemTop = currentItem.offset + dragOffset
            val itemBottom = itemTop + currentItem.size

            when {
                // 위쪽 경계 안 + 위로 드래그 중(dragDirection<0) + 더 스크롤할 여지가 있으면 → 위로 스크롤.
                // 방향 게이트가 없으면, 아이템이 상단 존에 들어간 순간 손가락을 아래로 내려도
                // 위 오토스크롤이 계속 발동해 아이템이 상단에 갇힌다.
                itemTop < viewStart + scrollZonePx && dragDirection < 0 && listState.canScrollBackward -> {
                    if (overscrollJob?.isActive == true) return
                    overscrollJob = coroutineScope.launch {
                        // 실제 스크롤된 양(consumed)만큼만 dragOffset 을 보정한다.
                        // 끝에 닿아 스크롤이 0이면 보정도 0 → 헛누적으로 인한 폭주를 막는다.
                        val consumed = listState.scrollBy(-scrollSpeedPx)
                        dragOffset += consumed
                    }
                }

                // 아래쪽 경계 안 + 아래로 드래그 중(dragDirection>0) + 더 스크롤할 여지가 있으면 → 아래로 스크롤.
                itemBottom > viewEnd - scrollZonePx && dragDirection > 0 && listState.canScrollForward -> {
                    if (overscrollJob?.isActive == true) return
                    overscrollJob = coroutineScope.launch {
                        val consumed = listState.scrollBy(scrollSpeedPx)
                        dragOffset += consumed
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
            scrollZonePx = with(density) { scrollZone.toPx() },
            scrollSpeedPx = with(density) { scrollSpeed.toPx() },
        )
    }
}
