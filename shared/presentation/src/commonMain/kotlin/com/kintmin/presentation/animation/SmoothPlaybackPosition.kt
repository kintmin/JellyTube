package com.kintmin.presentation.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

/**
 * 재생 위치는 Flow 가 없어 폴링한다. 기본은 RESUMED 동안만 돈다.
 * [whileStarted] 는 전환 애니메이션 중에도 보여야 하는 쪽이 쓴다 — RESUMED 는 전환이 끝나야 온다.
 */
@Composable
fun PlaybackPollingEffect(
    whileStarted: Boolean = false,
    onTick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val tick by rememberUpdatedState(onTick)
    fun start() = scope.launch {
        while (isActive) {
            tick()
            delay(PLAYBACK_POLLING_INTERVAL_MILLIS.toLong())
        }
    }
    if (whileStarted) {
        LifecycleStartEffect(Unit) {
            val job = start()
            onStopOrDispose { job.cancel() }
        }
    } else {
        LifecycleResumeEffect(Unit) {
            val job = start()
            onPauseOrDispose { job.cancel() }
        }
    }
}
