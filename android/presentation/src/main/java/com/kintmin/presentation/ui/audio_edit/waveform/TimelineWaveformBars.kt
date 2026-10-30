package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.presentation.theme.seaBlue40

/** 조각이 있는 곳에만 파형 막대를 그린다. 옮긴 조각은 원본 시각으로 환산해 그 자리 파형을 쓴다. */
internal fun DrawScope.drawWaveformBars(
    viewport: TimelineViewportState,
    waveform: List<Int>,
    timeline: AudioEditTimeline,
) {
    // 파형 — 픽셀 열마다 그 열이 덮는 원본 구간 버킷들의 최대값을 쓴다. 확대하면 버킷이 넓게 펼쳐진다.
    val originDurationMs = timeline.originDurationMs
    val peaks = waveform
    if (peaks.isNotEmpty() && originDurationMs > 0) {
        val maxPeak = (peaks.maxOrNull() ?: 0).coerceAtLeast(1)
        val centerY = size.height / 2f
        val barWidth = 2.dp.toPx()

        var x = 0f
        while (x < size.width) {
            val fromMs = viewport.msAt(x)
            val toMs = viewport.msAt(x + barWidth)
            val clip = timeline.clipIndexAt((fromMs + toMs) / 2)?.let { timeline.clips[it] }
            if (clip != null) {
                val peak = peaks.peakOf(
                    fromSourceMs = clip.sourceMsAt(fromMs),
                    toSourceMs = clip.sourceMsAt(toMs),
                    originDurationMs = originDurationMs,
                )
                val barHeight = (peak.toFloat() / maxPeak) * size.height * 0.9f
                drawRect(
                    color = seaBlue40,
                    topLeft = Offset(x = x, y = centerY - barHeight / 2f),
                    size = Size(width = barWidth * 0.7f, height = barHeight.coerceAtLeast(1f)),
                )
            }
            x += barWidth
        }
    }
}

/** 원본 [fromSourceMs]~[toSourceMs]에 해당하는 버킷들의 최대 진폭. */
private fun List<Int>.peakOf(fromSourceMs: Long, toSourceMs: Long, originDurationMs: Long): Int {
    val fromIndex = (fromSourceMs.coerceIn(0, originDurationMs) * size / originDurationMs).toInt()
        .coerceIn(0, lastIndex)
    val toIndex = (toSourceMs.coerceIn(0, originDurationMs) * size / originDurationMs).toInt()
        .coerceIn(fromIndex + 1, size)

    var peak = 0
    for (bucketIndex in fromIndex until toIndex) {
        if (this[bucketIndex] > peak) peak = this[bucketIndex]
    }
    return peak
}
