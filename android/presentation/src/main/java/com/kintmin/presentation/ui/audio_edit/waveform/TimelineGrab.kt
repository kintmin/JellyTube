package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.presentation.theme.black
import com.kintmin.presentation.theme.seaBlue100
import com.kintmin.presentation.theme.white

/**
 * 파형에서 꾹 눌러 잡은 대상. 손가락을 떼면 null이 된다.
 *
 * 잡혔다는 것을 화면에 보여주기 위한 표시 전용 상태라 ViewModel이 아니라 화면에 둔다.
 * [edge]가 null이면 조각을 통째로 옮기는 중이고, 아니면 그 경계로 길이를 조절하는 중이다.
 */
data class TimelineGrab(
    val clipIndex: Int,
    val edge: AudioEditTimeline.Edge?,
)

/**
 * 잡은 대상 강조. 두 모드를 색이 아니라 강조되는 모양으로 구분한다.
 *
 * - 이동: 잡은 조각이 들려 올라간 것처럼 밝아지고 테두리가 굵어지며, 다른 조각은 흐려진다
 * - 길이 조절: 잡은 경계에 위아래를 관통하는 세로선을 긋는다(손잡이 강조는 [drawClipGrips])
 */
internal fun DrawScope.drawGrabHighlight(
    viewport: TimelineViewportState,
    timeline: AudioEditTimeline,
    grab: TimelineGrab?,
) {
    val grabbedClip = grab?.let { timeline.clips.getOrNull(it.clipIndex) } ?: return
    val clipInset = 1.5.dp.toPx()
    val clipCorner = CornerRadius(4.dp.toPx())

    when (grab.edge) {
        null -> {
            timeline.clips.forEachIndexed { index, clip ->
                val left = viewport.xOf(clip.positionMs) + clipInset
                val right = viewport.xOf(clip.positionEndMs) - clipInset
                if (right < 0f || left > size.width) return@forEachIndexed
                val clipSize = Size(width = (right - left).coerceAtLeast(1f), height = size.height)

                if (index == grab.clipIndex) {
                    drawRoundRect(
                        color = white.copy(alpha = 0.12f),
                        topLeft = Offset(x = left, y = 0f),
                        size = clipSize,
                        cornerRadius = clipCorner,
                    )
                    drawRoundRect(
                        color = seaBlue100,
                        topLeft = Offset(x = left, y = 0f),
                        size = clipSize,
                        cornerRadius = clipCorner,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                } else {
                    drawRoundRect(
                        color = black.copy(alpha = 0.45f),
                        topLeft = Offset(x = left, y = 0f),
                        size = clipSize,
                        cornerRadius = clipCorner,
                    )
                }
            }
        }

        else -> {
            val edgeMs = when (grab.edge) {
                AudioEditTimeline.Edge.Start -> grabbedClip.positionMs
                AudioEditTimeline.Edge.End -> grabbedClip.positionEndMs
            }
            drawRect(
                color = white,
                topLeft = Offset(x = viewport.xOf(edgeMs) - 1.dp.toPx(), y = 0f),
                size = Size(width = 2.dp.toPx(), height = size.height),
            )
        }
    }
}
