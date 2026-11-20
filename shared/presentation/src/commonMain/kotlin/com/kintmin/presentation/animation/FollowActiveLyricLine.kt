package com.kintmin.presentation.animation

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

// 손을 뗀 뒤 이만큼 지나면 현재 줄로 돌아간다. 바로 돌아가면 앞뒤 가사를 읽을 수 없다.
private val RESUME_FOLLOW_DELAY = 3.seconds

/** 활성 줄을 화면 가운데쯤에 두기 위해 두 줄 앞을 맨 위로 올린다. */
private const val LINES_ABOVE_ACTIVE = 2

/** 활성 줄이 바뀌면 따라간다. 사용자가 끌어 스크롤하면 잠시 멈춘다. */
@Composable
fun FollowActiveLyricLine(
    listState: LazyListState,
    activeIndex: Int,
) {
    var isUserScrolling by remember { mutableStateOf(false) }

    // 끌기만 본다. 따라가는 animateScrollToItem 도 isScrollInProgress 를 켜므로 그것으로는 가를 수 없다.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect {
            if (it is DragInteraction.Start) isUserScrolling = true
        }
    }

    // 플링이 끝날 때까지 기다린 뒤 센다. 그 사이 다시 끌면 이 효과가 다시 시작돼 처음부터 센다.
    val isScrollInProgress = listState.isScrollInProgress
    LaunchedEffect(isUserScrolling, isScrollInProgress) {
        if (isUserScrolling && !isScrollInProgress) {
            delay(RESUME_FOLLOW_DELAY)
            isUserScrolling = false
        }
    }

    LaunchedEffect(activeIndex, isUserScrolling) {
        if (!isUserScrolling && activeIndex >= 0) {
            listState.animateScrollToItem((activeIndex - LINES_ABOVE_ACTIVE).coerceAtLeast(0))
        }
    }
}
