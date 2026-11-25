package com.kintmin.presentation.animation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

// 켜짐은 버튼을 누른 직후의 반응이라 빠르게 붙고, 꺼짐은 그보다 느긋하게 빠진다.
private const val GLOW_ENTER_MILLIS = 150
private const val GLOW_EXIT_MILLIS = 300

/**
 * 번짐이 가장 셀 때의 알파. 쓰는 쪽이 가산으로 얹으므로 어두운 바닥에서는 이 값이 그대로
 * 밝기로 더해진다 — 스크롤되는 글자 위로도 번지니 낮게 둔다.
 */
const val GLOW_MAX_ALPHA = 0.35f

/**
 * 재생 중 번짐의 세기(0~1).
 *
 * 번짐을 `Modifier.blur` 로 만들지 않는다 — Android 12 미만에서 **아무 효과 없이 조용히
 * 무시되어** 그 기기에서만 재생 신호가 사라진다. 쓰는 쪽이 그라데이션으로 직접 그린다.
 */
@Composable
fun animatePlayingGlow(isPlaying: Boolean): Float {
    val glow by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isPlaying) GLOW_ENTER_MILLIS else GLOW_EXIT_MILLIS,
            easing = if (isPlaying) LinearOutSlowInEasing else FastOutLinearInEasing,
        ),
        label = "playingGlow",
    )
    return glow
}
