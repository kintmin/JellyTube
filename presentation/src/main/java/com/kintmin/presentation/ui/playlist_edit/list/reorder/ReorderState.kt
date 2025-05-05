package com.kintmin.presentation.ui.playlist_edit.list.reorder

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListItemUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReorderState(
    private val density: Density,
    private val coroutineScope: CoroutineScope,
    val listState: LazyListState,
    val audioPlayList: SnapshotStateList<PlaylistEditListItemUiState>,
    initializeItemHeightPx: Float,
) {
    private var itemHeightPx by mutableFloatStateOf(initializeItemHeightPx)
    private var dragOffset by mutableFloatStateOf(0f)
    private var draggingItemOffset by mutableIntStateOf(0)
    private var overscrollJob by mutableStateOf<Job?>(null)

    var draggingItemId by mutableStateOf<Int?>(null)

    fun onDragStart(offset: Offset, targetId: Int) {
        dragOffset = offset.y
        draggingItemId = targetId

        listState.layoutInfo.visibleItemsInfo.find { it.key == targetId }?.let { info ->
            draggingItemOffset = info.offset
        }
    }

    fun onDrag(change: PointerInputChange, dragAmount: Offset) {
        change.consume()
        dragOffset += dragAmount.y

        val moved = if (dragOffset > 0) {
            (dragOffset / itemHeightPx)
        } else {
            ((dragOffset - itemHeightPx) / itemHeightPx)
        }.toInt()

        val fromIndex = audioPlayList.indexOfFirst { it.id == draggingItemId }
        if (fromIndex == -1) return

        val toIndex = (fromIndex + moved).coerceIn(0, audioPlayList.lastIndex)

        if (fromIndex != toIndex) {
            audioPlayList.add(toIndex, audioPlayList.removeAt(fromIndex))
            dragOffset -= (moved * itemHeightPx)
        }

        // 오토 스크롤
        val scrollZone = with(density) { 80.dp.toPx() }
        val scrollSpeed = with(density) { 12.dp.toPx() }

        val currentItem = listState.layoutInfo.visibleItemsInfo.find { it.key == draggingItemId }

        val canScrollDown = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.let { lastItem ->
            val bottomReached = lastItem.offset + lastItem.size <= listState.layoutInfo.viewportEndOffset
            !(bottomReached)
        } ?: false

        if (currentItem != null) {
            val itemTop = currentItem.offset
            val itemBottom = itemTop + currentItem.size

            val viewStart = listState.layoutInfo.viewportStartOffset
            val viewEnd = listState.layoutInfo.viewportEndOffset

            when {
                itemTop < viewStart + scrollZone -> {
                    if (overscrollJob?.isActive == true) return
                    overscrollJob = coroutineScope.launch {
                        listState.scrollBy(-scrollSpeed)
                        dragOffset -= scrollSpeed
                    }
                }

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

    fun getDraggingItemIndex(): Int? {
        return audioPlayList.indexOfFirst { it.id == draggingItemId }.takeIf { it != -1 }
    }
}

@Composable
fun rememberReorderState(
    audioPlayDataList: List<PlaylistEditListItemUiState>,
    initializeItemHeightPx: Dp,
): ReorderState {
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val audioPlayList = remember { mutableStateListOf(*audioPlayDataList.toTypedArray()) }

    return remember {
        ReorderState(
            density = density,
            listState = listState,
            coroutineScope = coroutineScope,
            audioPlayList = audioPlayList,
            initializeItemHeightPx = with(density) { initializeItemHeightPx.toPx() },
        )
    }
}