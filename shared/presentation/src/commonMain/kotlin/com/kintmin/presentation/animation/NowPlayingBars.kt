package com.kintmin.presentation.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.seaBlue100

private val BAR_WIDTH = 3.dp
private val BAR_GAP = 2.dp
private val BAR_MAX_HEIGHT = 16.dp

// 기둥마다 주기를 달리해 위상을 흩뜨린다. 같으면 세 기둥이 한 덩어리로 오르내린다.
private val BAR_PERIODS_MILLIS = listOf(600, 450, 750)

private const val MIN_LEVEL = 0.3f

// 멈춘 기둥은 "현재 곡" 만 뜻한다. 높이를 서로 달리 둬야 정지 상태로 읽힌다.
private val PAUSED_LEVELS = listOf(0.45f, 0.75f, 0.35f)

/**
 * 재생 중인 곡을 가리키는 기둥 세 개. [isPlaying] 이 아니면 멈춘 높이로 고정한다 —
 * 움직임 자체가 재생/일시정지를 가르는 신호이므로 색으로만 전하지 않는다.
 */
@Composable
fun NowPlayingBars(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
) {
    val color = seaBlue100

    if (!isPlaying) {
        // 애니메이션을 걸지 않는다. 걸면 보이지도 않는 변화로 프레임 클럭이 계속 깨어 있다.
        Canvas(modifier = modifier) { drawNowPlayingBars(PAUSED_LEVELS, color) }
        return
    }

    val transition = rememberInfiniteTransition(label = "nowPlayingBars")
    val levels = BAR_PERIODS_MILLIS.map { periodMillis ->
        transition.animateFloat(
            initialValue = MIN_LEVEL,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = periodMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "nowPlayingBar$periodMillis",
        )
    }

    Canvas(modifier = modifier) {
        // 프레임마다 바뀌는 값은 여기서만 읽는다. 바깥에서 읽으면 행 전체가 매 프레임 다시 그려진다.
        drawNowPlayingBars(levels.map { it.value }, color)
    }
}

private fun DrawScope.drawNowPlayingBars(levels: List<Float>, color: Color) {
    val barWidth = BAR_WIDTH.toPx()
    val gap = BAR_GAP.toPx()
    val maxHeight = BAR_MAX_HEIGHT.toPx()
    val totalWidth = (barWidth * levels.size) + (gap * (levels.size - 1))
    val startX = (size.width - totalWidth) / 2f
    val bottomY = (size.height + maxHeight) / 2f

    levels.forEachIndexed { index, level ->
        val height = maxHeight * level.coerceIn(0f, 1f)
        drawRect(
            color = color,
            topLeft = Offset(startX + (index * (barWidth + gap)), bottomY - height),
            size = Size(barWidth, height),
        )
    }
}

@Preview
@Composable
fun NowPlayingBarsPlayingPreview() {
    JellyTubeTheme {
        NowPlayingBars(
            modifier = Modifier.size(64.dp),
            isPlaying = true,
        )
    }
}

@Preview
@Composable
fun NowPlayingBarsPausedPreview() {
    JellyTubeTheme {
        NowPlayingBars(
            modifier = Modifier.size(64.dp),
            isPlaying = false,
        )
    }
}
