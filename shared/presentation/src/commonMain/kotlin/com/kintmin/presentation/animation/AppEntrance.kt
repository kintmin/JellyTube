package com.kintmin.presentation.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme

private const val APP_ENTRANCE_MILLIS = 250
private val AppEntranceOffset = 16.dp

/** 첫 진입에만 내용을 페이드·살짝 올라오게 띄운다. 그리는 단계만 바꾸므로 터치는 처음부터 받는다. */
@Composable
fun AppEntrance(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // 액티비티 재생성(회전 등)에서 다시 재생하지 않게 저장한다.
    var hasEntered by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (hasEntered) 1f else 0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(APP_ENTRANCE_MILLIS, easing = LinearOutSlowInEasing))
        hasEntered = true
    }
    val offsetPx = with(LocalDensity.current) { AppEntranceOffset.toPx() }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * offsetPx
        },
    ) {
        content()
    }
}

@Preview
@Composable
private fun AppEntrancePreview() {
    JellyTubeTheme {
        AppEntrance(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "JellyTube")
            }
        }
    }
}
