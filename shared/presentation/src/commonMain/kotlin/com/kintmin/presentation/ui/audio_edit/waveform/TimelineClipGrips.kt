package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.seaBlue100
import com.kintmin.presentation.theme.white

/**
 * 선택한 조각 양 끝의 손잡이와 가운데 이동 표시.
 *
 * 선택한 조각에만 그린다. 손잡이가 보이는 경계는 바로 끌리고, 선택하지 않은 조각은 꾹 눌러야
 * 잡히므로 손잡이를 그리면 바로 끌릴 것처럼 보인다.
 * 길이 조절 중에는 잡은 쪽 손잡이만 크고 밝게 바꿔, 어느 경계가 잡혔는지 보여준다.
 * 손잡이는 실제로 경계를 잡는 범위([edgeTouchRadiusPx]) 안쪽에 그린다. 조각이 너무 좁아
 * 손잡이나 이동 표시를 그릴 자리가 없으면 생략한다.
 */
internal fun DrawScope.drawClipGrips(
    viewport: TimelineViewportState,
    timeline: AudioEditTimeline,
    selectedClipIndex: Int?,
    grab: TimelineGrab?,
    moveHandlePainter: Painter,
) {
    val gripWidth = 3.dp.toPx()
    val gripHeight = 24.dp.toPx()
    val gripInset = 4.dp.toPx()
    val grabbedGripWidth = 5.dp.toPx()
    val grabbedGripHeight = 40.dp.toPx()
    val maxRadiusPx = EDGE_TOUCH_RADIUS.toPx()

    val moveHandleSize = 20.dp.toPx()

    // 병합한 묶음은 한 덩이라 안쪽 경계에는 손잡이가 없다
    val group = selectedClipIndex?.let(timeline::groupRangeOf) ?: return
    val left = viewport.xOf(timeline.clips[group.first].positionMs)
    val right = viewport.xOf(timeline.clips[group.last].positionEndMs)
    if (right < 0f || left > size.width) return

    val radiusPx = edgeTouchRadiusPx(clipWidthPx = right - left, maxRadiusPx = maxRadiusPx)
    if (radiusPx < gripInset + gripWidth) return

    listOf(
        Triple(AudioEditTimeline.Edge.Start, group.first, left + gripInset),
        Triple(AudioEditTimeline.Edge.End, group.last, right - gripInset),
    ).forEach { (edge, clipIndex, edgeX) ->
        val isGrabbed = grab?.clipIndex == clipIndex && grab.edge == edge
        val width = if (isGrabbed) grabbedGripWidth else gripWidth
        val height = if (isGrabbed) grabbedGripHeight else gripHeight
        drawRoundRect(
            color = if (isGrabbed) white else seaBlue100,
            topLeft = Offset(
                x = if (edge == AudioEditTimeline.Edge.Start) edgeX else edgeX - width,
                y = (size.height - height) / 2f,
            ),
            size = Size(width = width, height = height),
            cornerRadius = CornerRadius(width / 2f),
        )
    }

    // 가운데 절반은 늘 옮기기용으로 남아 있어, 그 폭에 들어갈 때만 이동 표시를 그린다
    if ((right - left) / 2f < moveHandleSize) return
    val isMoving = grab != null && grab.clipIndex in group && grab.edge == null
    translate(
        left = (left + right - moveHandleSize) / 2f,
        top = (size.height - moveHandleSize) / 2f,
    ) {
        with(moveHandlePainter) {
            draw(
                size = Size(moveHandleSize, moveHandleSize),
                colorFilter = ColorFilter.tint(if (isMoving) white else seaBlue100),
            )
        }
    }
}

/**
 * 원본에서 이어지지 않은 조각을 병합한 이음매를 흐린 점선으로 보여 준다.
 *
 * 묶음은 한 덩이로 움직이지만, 여기서 자르면 병합이 풀린다는 것을 알 수 있게 자리를 남긴다.
 * 원본에서 이어진 조각끼리 병합하면 아예 한 조각이 되어 이음매가 없다.
 */
internal fun DrawScope.drawMergeSeams(
    viewport: TimelineViewportState,
    timeline: AudioEditTimeline,
) {
    val dashPx = 4.dp.toPx()
    timeline.seamPositionsMs.forEach { seamMs ->
        val x = viewport.xOf(seamMs)
        if (x < 0f || x > size.width) return@forEach
        drawLine(
            color = gray60.copy(alpha = 0.7f),
            start = Offset(x = x, y = 0f),
            end = Offset(x = x, y = size.height),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx)),
        )
    }
}
