package com.kintmin.presentation.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.math.abs

/** 재생 화면들이 위치를 폴링하는 간격. 보간이 이 시간 동안 다음 값까지 가야 끊김 없이 이어진다. */
private const val POLLING_INTERVAL_MILLIS = 300

/** 이보다 크게 바뀌면 재생으로 흐른 게 아니라 시크·곡 전환이라 미끄러지지 않고 바로 붙는다(3배속 폴링 한 번 ≈ 0.9초). */
private const val MAX_INTERPOLATION_GAP_SECONDS = 1.5f

/**
 * 폴링으로 띄엄띄엄 들어오는 재생 위치(초)를 그 사이에서 선형으로 이어 준다.
 *
 * [snap] 이면(드래그 중·일시정지) 보간 없이 바로 그 값을 쓴다.
 */
@Composable
fun animatePlaybackSeconds(targetSeconds: Float, snap: Boolean): Float {
    val animatable = remember { Animatable(targetSeconds) }
    LaunchedEffect(targetSeconds, snap) {
        if (snap || abs(targetSeconds - animatable.value) > MAX_INTERPOLATION_GAP_SECONDS) {
            animatable.snapTo(targetSeconds)
        } else {
            animatable.animateTo(
                targetValue = targetSeconds,
                animationSpec = tween(durationMillis = POLLING_INTERVAL_MILLIS, easing = LinearEasing),
            )
        }
    }
    return animatable.value
}
