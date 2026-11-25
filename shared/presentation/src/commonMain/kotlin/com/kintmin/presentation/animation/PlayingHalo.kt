package com.kintmin.presentation.animation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.artworkAccentFallback

private val HALO_SPREAD = 8.dp

// 겹친 고리의 수. 적으면 계단이 보이고, 많아도 그라데이션은 더 고와지지 않는다.
private const val HALO_RINGS = 8

/**
 * [shape] 둘레로 번지는 재생 중 빛. 네 모서리가 같은 모양이라고 본다. [glow] 는 그리기 안에서만 읽는다.
 *
 * `clip` 보다 **앞에** 둔다 — 뒤에 두면 번짐이 잘린다. 부모가 자르는 범위 안까지만 번지므로
 * 둘레에 [HALO_SPREAD] 만큼 여백이 있어야 다 보인다.
 */
fun Modifier.playingHalo(glow: State<Float>, color: Color, shape: CornerBasedShape): Modifier = drawBehind {
    val strength = glow.value
    if (strength <= 0f) return@drawBehind

    val ringWidth = HALO_SPREAD.toPx() / HALO_RINGS
    val radius = shape.topStart.toPx(size, this)
    repeat(HALO_RINGS) { ring ->
        val inset = ringWidth * (ring + 0.5f)
        // 안쪽 고리일수록 진하다. 가산이라 겹친 만큼 밝아진다.
        val falloff = 1f - (ring.toFloat() / HALO_RINGS)
        drawRoundRect(
            color = color.copy(alpha = GLOW_MAX_ALPHA * strength * falloff * falloff),
            topLeft = Offset(-inset, -inset),
            size = Size(size.width + inset * 2, size.height + inset * 2),
            cornerRadius = CornerRadius(radius + inset),
            style = Stroke(width = ringWidth),
            blendMode = BlendMode.Plus,
        )
    }
}

@Preview
@Composable
fun PlayingHaloPreview() {
    JellyTubeTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .playingHalo(animatePlayingGlow(isPlaying = true), artworkAccentFallback, MaterialTheme.shapes.small)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}
