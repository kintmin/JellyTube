package com.kintmin.presentation.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import kotlin.math.PI
import kotlin.math.sin

private const val WAVE_REVEAL_MILLIS = 450
// 정수 폭이라야 띠가 픽셀에 맞아 이음매가 겹치거나 비지 않는다.
private const val STRIP_PX = 6
private const val TAU = (2 * PI).toFloat()
private val WaveAmplitude = 28.dp
private val FeatherHeight = 96.dp

/**
 * [revealed] 가 켜질 때 아래에서 물결이 차오르며 내용을 드러낸다. 경계는 위로 갈수록 투명해진다.
 * 처음부터 켜져 있으면 재생하지 않는다. 그리는 단계만 바꾸므로 터치는 처음부터 받는다.
 */
@Composable
fun WaveReveal(
    revealed: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(WAVE_REVEAL_MILLIS, easing = FastOutSlowInEasing),
    )
    val crest = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .graphicsLayer {
                // 마스크(DstIn)가 화면 전체가 아니라 이 레이어의 내용에만 먹게 한다.
                compositingStrategy =
                    if (progress.value < 1f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            }
            .drawWithContent {
                val p = progress.value
                if (p >= 1f) {
                    drawContent()
                    return@drawWithContent
                }
                if (p <= 0f) return@drawWithContent
                drawContent()

                val width = size.width
                val height = size.height
                val maxAmplitude = WaveAmplitude.toPx()
                val feather = FeatherHeight.toPx()
                // 차오를수록 잔잔해진다. 시작에는 깃털까지 화면 아래, 끝에는 물결이 화면 위로 빠진다.
                val amplitude = maxAmplitude * (1f - p * p)
                val baseline = (height + maxAmplitude + feather) - (height + 2f * maxAmplitude + feather) * p
                val phase = p * TAU * 1.4f
                val glow = crest.copy(alpha = 0.2f * (1f - p))

                var x = 0
                while (x < width) {
                    val t = (x + STRIP_PX / 2f) / width
                    val y = baseline + amplitude *
                        (0.7f * sin(t * TAU * 1.2f + phase) + 0.3f * sin(t * TAU * 2.7f - phase * 1.6f))
                    val top = y - feather
                    val stripSize = Size(STRIP_PX.toFloat(), height)
                    val stripOffset = Offset(x.toFloat(), 0f)
                    // 마스크는 알파만 쓴다. 색은 무엇이든 불투명하면 된다.
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, crest),
                            startY = top,
                            endY = y,
                        ),
                        topLeft = stripOffset,
                        size = stripSize,
                        blendMode = BlendMode.DstIn,
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.7f to glow,
                            1f to Color.Transparent,
                            startY = top,
                            endY = y + feather * 0.3f,
                        ),
                        topLeft = stripOffset,
                        size = stripSize,
                    )
                    x += STRIP_PX
                }
            },
    ) {
        content()
    }
}

@Preview
@Composable
private fun WaveRevealPreview() {
    JellyTubeTheme {
        WaveReveal(
            revealed = true,
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
