package com.kintmin.presentation.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.math.abs

const val PLAYBACK_POLLING_INTERVAL_MILLIS = 300

/** 3배속에서 폴링 한 번에 0.9초 흐른다. 이보다 크게 바뀌면 시크·곡 전환이다. */
private const val MAX_INTERPOLATION_GAP_SECONDS = 1.5f

/** 폴링 사이를 선형으로 잇는다. [snap] 이면 보간 없이 바로 붙는다. */
@Composable
fun animatePlaybackSeconds(targetSeconds: Float, snap: Boolean): Float {
    val animatable = remember { Animatable(targetSeconds) }
    LaunchedEffect(targetSeconds, snap) {
        if (snap || abs(targetSeconds - animatable.value) > MAX_INTERPOLATION_GAP_SECONDS) {
            animatable.snapTo(targetSeconds)
        } else {
            animatable.animateTo(
                targetValue = targetSeconds,
                animationSpec = tween(durationMillis = PLAYBACK_POLLING_INTERVAL_MILLIS, easing = LinearEasing),
            )
        }
    }
    return animatable.value
}
