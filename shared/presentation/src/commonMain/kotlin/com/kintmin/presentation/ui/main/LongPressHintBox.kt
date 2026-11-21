package com.kintmin.presentation.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.kintmin.presentation.animation.FocusSpreadOverlay
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * 손가락이 눌러 머물다 떼는 모양을 [content] 에 두 번 보여 주고, 그동안 강조 물결을 퍼뜨린다.
 * 한 번 보여 주면 화면을 다시 만들어도 되풀이하지 않는다.
 */
@Composable
internal fun LongPressHintBox(
    isHintRequested: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var isConsumed by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isHintRequested) {
        if (!isHintRequested || isConsumed) return@LaunchedEffect
        isConsumed = true
        try {
            // 화면 전환이 끝난 뒤에 시작해야 보인다.
            delay(START_DELAY)
            repeat(REPEAT_COUNT) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(durationMillis = DURATION_MILLIS, easing = LinearEasing))
            }
        } finally {
            // 도중에 탭을 바꿔 취소돼도 눌린 채로 남지 않게 한다.
            withContext(NonCancellable) { progress.snapTo(0f) }
        }
    }

    val value = progress.value
    Box(modifier = modifier) {
        Box(
            modifier = Modifier.graphicsLayer {
                val scale = 1f - PRESS_DEPTH * pressAt(value)
                scaleX = scale
                scaleY = scale
            },
        ) {
            content()
        }
        if (value > 0f) {
            FocusSpreadOverlay(
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.small),
                progress = value,
            )
        }
    }
}

/** 0 은 놓은 상태, 1 은 끝까지 눌린 상태. 빠르게 눌러 오래 머문 뒤 놓는다. */
internal fun pressAt(progress: Float): Float = when {
    progress <= 0f || progress >= RELEASE_END -> 0f
    progress < PRESS_END -> progress / PRESS_END
    progress < HOLD_END -> 1f
    else -> (RELEASE_END - progress) / (RELEASE_END - HOLD_END)
}

private const val PRESS_END = 0.15f
private const val HOLD_END = 0.7f
private const val RELEASE_END = 0.85f
private const val PRESS_DEPTH = 0.08f
private const val DURATION_MILLIS = 1400
private const val REPEAT_COUNT = 2
private val START_DELAY = 450.milliseconds
