package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.seaBlue100
import com.kintmin.presentation.theme.white

/**
 * 조각 양 끝의 손잡이. 꾹 눌러 끌면 길이가 바뀌는 자리라는 것을 보여준다.
 *
 * 길이 조절 중에는 잡은 쪽 손잡이만 크고 밝게 바꿔, 어느 경계가 잡혔는지 보여준다.
 * 손잡이는 실제로 경계를 잡는 범위([edgeTouchRadiusPx]) 안쪽에 그린다. 조각이 너무 좁아
 * 손잡이를 그릴 자리가 없으면 생략한다.
 */
internal fun DrawScope.drawClipGrips(
    viewport: TimelineViewportState,
    timeline: AudioEditTimeline,
    selectedClipIndex: Int?,
    grab: TimelineGrab?,
) {
    val gripWidth = 3.dp.toPx()
    val gripHeight = 24.dp.toPx()
    val gripInset = 4.dp.toPx()
    val grabbedGripWidth = 5.dp.toPx()
    val grabbedGripHeight = 40.dp.toPx()
    val maxRadiusPx = EDGE_TOUCH_RADIUS.toPx()

    // 병합한 묶음은 한 덩이라 안쪽 경계에는 손잡이가 없다
    timeline.groupRanges.forEach { group ->
        val left = viewport.xOf(timeline.clips[group.first].positionMs)
        val right = viewport.xOf(timeline.clips[group.last].positionEndMs)
        if (right < 0f || left > size.width) return@forEach

        val radiusPx = edgeTouchRadiusPx(clipWidthPx = right - left, maxRadiusPx = maxRadiusPx)
        if (radiusPx < gripInset + gripWidth) return@forEach

        val color = if (selectedClipIndex in group) seaBlue100 else gray60.copy(alpha = 0.7f)
        listOf(
            Triple(AudioEditTimeline.Edge.Start, group.first, left + gripInset),
            Triple(AudioEditTimeline.Edge.End, group.last, right - gripInset),
        ).forEach { (edge, clipIndex, edgeX) ->
            val isGrabbed = grab?.clipIndex == clipIndex && grab.edge == edge
            val width = if (isGrabbed) grabbedGripWidth else gripWidth
            val height = if (isGrabbed) grabbedGripHeight else gripHeight
            drawRoundRect(
                color = if (isGrabbed) white else color,
                // 끝 쪽 손잡이는 오른쪽 가장자리를 기준으로 안쪽에 그린다
                topLeft = Offset(
                    x = if (edge == AudioEditTimeline.Edge.Start) edgeX else edgeX - width,
                    y = (size.height - height) / 2f,
                ),
                size = Size(width = width, height = height),
                cornerRadius = CornerRadius(width / 2f),
            )
        }
    }
}
