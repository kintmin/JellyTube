package com.kintmin.presentation.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** 의미를 지닌 그래픽의 대비 바닥값. */
private const val GRAPHIC_CONTRAST_FLOOR = 3f

/** 흰색 쪽으로 끌어올리는 단계. 작을수록 원래 색조에 가깝게 멈춘다. */
private const val LIFT_STEP = 0.02f

fun Color.lighten(factor: Float): Color {
    val safe = factor.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * safe,
        green = green + (1f - green) * safe,
        blue = blue + (1f - blue) * safe,
        alpha = 1f,
    )
}

fun Color.darken(factor: Float): Color {
    val safe = factor.coerceIn(0f, 1f)
    val mul = 1f - safe
    return Color(
        red = red * mul,
        green = green * mul,
        blue = blue * mul,
        alpha = 1f,
    )
}

/**
 * 대표색은 "가장 많이 쓰인 색" 일 뿐 밝기가 보장되지 않아, 그대로 강조색으로 쓰면 어두운
 * 앨범아트에서 진행 표시가 바닥에 묻는다. 색조를 두고 흰색 쪽으로만 끌어올려
 * [accentContrastSurface] 와의 대비 바닥값을 넘긴 첫 색을 돌려준다.
 */
fun Color.liftedForAccent(): Color {
    var factor = 0f
    while (factor < 1f) {
        val candidate = lighten(factor)
        if (contrastRatio(candidate, accentContrastSurface) >= GRAPHIC_CONTRAST_FLOOR) {
            return candidate
        }
        factor += LIFT_STEP
    }
    return lighten(1f)
}

/** WCAG 2 대비비. 밝은 쪽이 분자다. */
internal fun contrastRatio(a: Color, b: Color): Float {
    val one = a.relativeLuminance()
    val other = b.relativeLuminance()
    return (maxOf(one, other) + 0.05f) / (minOf(one, other) + 0.05f)
}

private fun Color.relativeLuminance(): Float =
    (0.2126f * linearize(red)) + (0.7152f * linearize(green)) + (0.0722f * linearize(blue))

private fun linearize(channel: Float): Float =
    if (channel <= 0.03928f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)
