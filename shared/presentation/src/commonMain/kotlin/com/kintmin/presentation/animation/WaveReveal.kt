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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import kotlin.math.PI
import kotlin.math.sin

private const val WAVE_REVEAL_MILLIS = 700
private const val WAVE_STEP_PX = 8f
private const val TAU = (2 * PI).toFloat()
private val WaveAmplitude = 28.dp
private val CrestWidth = 3.dp
private val CrestGlowWidth = 20.dp

/**
 * [revealed] 가 켜질 때 아래에서 물결 경계가 차오르며 내용을 드러낸다. 처음부터 켜져 있으면 재생하지 않는다.
 * 그리는 단계만 바꾸므로 터치는 처음부터 받는다.
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
    val edge = remember { Path() }
    val filled = remember { Path() }

    Box(
        modifier = modifier.drawWithContent {
            val p = progress.value
            if (p >= 1f) {
                drawContent()
                return@drawWithContent
            }
            if (p <= 0f) return@drawWithContent

            val width = size.width
            val height = size.height
            val maxAmplitude = WaveAmplitude.toPx()
            // 차오를수록 잔잔해진다. 끝에서 진폭이 0 이라 경계가 화면 위로 빠져 전부 보인다.
            val amplitude = maxAmplitude * (1f - p * p)
            val baseline = (height + maxAmplitude) - (height + 2f * maxAmplitude) * p
            val phase = p * TAU * 1.4f

            edge.reset()
            var x = 0f
            while (true) {
                val t = x / width
                val y = baseline + amplitude *
                    (0.7f * sin(t * TAU * 1.2f + phase) + 0.3f * sin(t * TAU * 2.7f - phase * 1.6f))
                if (x == 0f) edge.moveTo(x, y) else edge.lineTo(x, y)
                if (x >= width) break
                x = (x + WAVE_STEP_PX).coerceAtMost(width)
            }

            filled.reset()
            filled.addPath(edge)
            filled.lineTo(width, height)
            filled.lineTo(0f, height)
            filled.close()
            clipPath(filled) { this@drawWithContent.drawContent() }

            val fade = 1f - p
            drawPath(
                path = edge,
                color = crest.copy(alpha = 0.14f * fade),
                style = Stroke(width = CrestGlowWidth.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                path = edge,
                color = crest.copy(alpha = 0.7f * fade),
                style = Stroke(width = CrestWidth.toPx(), cap = StrokeCap.Round),
            )
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
