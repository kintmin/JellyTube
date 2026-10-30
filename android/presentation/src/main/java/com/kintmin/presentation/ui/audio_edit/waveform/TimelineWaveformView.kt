package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.domain.audio_edit.model.AudioEditClip
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.model.AudioWaveform
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.black
import com.kintmin.presentation.theme.bloodyRed100
import com.kintmin.presentation.theme.deepSea40
import com.kintmin.presentation.theme.deepSea80
import com.kintmin.presentation.theme.gray10
import com.kintmin.presentation.theme.seaBlue100
import com.kintmin.presentation.theme.seaBlue40
import com.kintmin.presentation.theme.seaBlue60
import com.kintmin.presentation.theme.white

/**
 * 편집 타임라인의 파형 영역. 조각을 고르고, 화면을 옮기고, 경계를 끌고, 조각을 옮기는 곳이다.
 *
 * - 0초~끝 표시는 트랙 배경을 깔아, 빈칸과 뒤에 붙인 무음도 "비어 있는 트랙"으로 보이게 한다
 * - 조각은 양옆을 조금씩 띄운 둥근 블록으로 그려, 맞닿은 조각도 경계가 보이게 한다
 * - 결과물 끝 뒤는 어둡게 덮고 끝 표시(빨간 선)를 긋는다
 * - 기준선(흰색)과 미리듣기선(파란색)은 별개다
 *
 * 높이는 화면 높이에 따라 달라지므로 호출하는 쪽에서 정한다.
 * 제스처 구분은 [timelineWaveformGestures]에 있다.
 */
@Composable
fun TimelineWaveformView(
    modifier: Modifier,
    viewport: TimelineViewportState,
    waveform: AudioWaveform,
    timeline: AudioEditTimeline,
    selectedClipIndex: Int?,
    grab: TimelineGrab?,
    baselineMs: Long,
    previewPositionMs: Long?,
    onClipSelected: (Int?) -> Unit,
    onClipEdgeDragged: (index: Int, edge: AudioEditTimeline.Edge, ms: Long) -> Unit,
    onClipMoved: (index: Int, positionMs: Long) -> Unit,
    onGrabChanged: (TimelineGrab?) -> Unit,
) {
    val currentTimeline by rememberUpdatedState(timeline)
    val currentOnClipSelected by rememberUpdatedState(onClipSelected)
    val currentOnClipEdgeDragged by rememberUpdatedState(onClipEdgeDragged)
    val currentOnClipMoved by rememberUpdatedState(onClipMoved)
    val currentOnGrabChanged by rememberUpdatedState(onGrabChanged)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { viewport.updateWidth(it.width.toFloat()) }
            .pointerInput(viewport) {
                timelineWaveformGestures(
                    viewport = viewport,
                    timeline = { currentTimeline },
                    onTap = { ms -> currentOnClipSelected(currentTimeline.clipIndexAt(ms)) },
                    onGrabStarted = { grab ->
                        // 잡은 조각을 바로 선택해 두면 놓은 뒤 곧장 삭제할 수 있다
                        currentOnClipSelected(grab.clipIndex)
                        currentOnGrabChanged(grab)
                    },
                    onGrabEnded = { currentOnGrabChanged(null) },
                    onEdgeDragged = { index, edge, ms -> currentOnClipEdgeDragged(index, edge, ms) },
                    onClipMoved = { index, positionMs -> currentOnClipMoved(index, positionMs) },
                )
            },
    ) {
        val endX = viewport.xOf(timeline.endMs)

        // 트랙 배경(0초~끝 표시)
        val trackLeft = viewport.xOf(0).coerceAtLeast(0f)
        val trackRight = endX.coerceAtMost(size.width)
        if (trackRight > trackLeft) {
            drawRect(
                color = gray10,
                topLeft = Offset(x = trackLeft, y = 0f),
                size = Size(width = trackRight - trackLeft, height = size.height),
            )
        }

        // 조각 블록 — 맞닿은 조각도 틈이 보이도록 양옆을 띄운다
        val clipInset = 1.5.dp.toPx()
        val clipCorner = CornerRadius(4.dp.toPx())
        timeline.clips.forEachIndexed { index, clip ->
            val left = viewport.xOf(clip.positionMs) + clipInset
            val right = viewport.xOf(clip.positionEndMs) - clipInset
            if (right < 0f || left > size.width) return@forEachIndexed

            val isSelected = index == selectedClipIndex
            val clipSize = Size(width = (right - left).coerceAtLeast(1f), height = size.height)
            drawRoundRect(
                color = if (isSelected) deepSea40 else deepSea80,
                topLeft = Offset(x = left, y = 0f),
                size = clipSize,
                cornerRadius = clipCorner,
            )
            drawRoundRect(
                color = if (isSelected) seaBlue100 else seaBlue40.copy(alpha = 0.5f),
                topLeft = Offset(x = left, y = 0f),
                size = clipSize,
                cornerRadius = clipCorner,
                style = Stroke(width = if (isSelected) 2.dp.toPx() else 1.dp.toPx()),
            )
        }

        drawWaveformBars(viewport = viewport, waveform = waveform, timeline = timeline)
        drawClipGrips(viewport = viewport, timeline = timeline, selectedClipIndex = selectedClipIndex, grab = grab)
        drawGrabHighlight(viewport = viewport, timeline = timeline, grab = grab)

        // 결과물 끝 뒤는 저장되지 않는다
        if (endX < size.width) {
            val dimLeft = endX.coerceAtLeast(0f)
            drawRect(
                color = black.copy(alpha = 0.55f),
                topLeft = Offset(x = dimLeft, y = 0f),
                size = Size(width = size.width - dimLeft, height = size.height),
            )
            drawRect(
                color = bloodyRed100,
                topLeft = Offset(x = endX - 1.dp.toPx(), y = 0f),
                size = Size(width = 2.dp.toPx(), height = size.height),
            )
        }

        // 기준선
        drawRect(
            color = white,
            topLeft = Offset(x = viewport.xOf(baselineMs) - 1.dp.toPx() / 2f, y = 0f),
            size = Size(width = 1.dp.toPx(), height = size.height),
        )

        // 미리듣기선
        previewPositionMs?.let { positionMs ->
            drawRect(
                color = seaBlue60,
                topLeft = Offset(x = viewport.xOf(positionMs) - 1.dp.toPx(), y = 0f),
                size = Size(width = 2.dp.toPx(), height = size.height),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF191919)
@Composable
fun TimelineWaveformViewPreview() {
    JellyTubeTheme {
        val viewport = rememberTimelineViewportState().apply { updateContent(330_000) }
        TimelineWaveformView(
            modifier = Modifier.height(160.dp),
            viewport = viewport,
            waveform = AudioWaveform(
                peaks = List(3_000) { index -> 3000 + (index * 37 % 29_000) },
                loadedBucketCount = 2_400,
            ),
            timeline = AudioEditTimeline(
                originDurationMs = 300_000,
                clips = listOf(
                    AudioEditClip(sourceStartMs = 0, sourceEndMs = 60_000, positionMs = 0),
                    AudioEditClip(sourceStartMs = 60_000, sourceEndMs = 120_000, positionMs = 60_000),
                    AudioEditClip(sourceStartMs = 180_000, sourceEndMs = 300_000, positionMs = 200_000),
                ),
                endMs = 310_000,
            ),
            selectedClipIndex = 1,
            grab = TimelineGrab(clipIndex = 1, edge = null),
            baselineMs = 30_000,
            previewPositionMs = 220_000,
            onClipSelected = {},
            onClipEdgeDragged = { _, _, _ -> },
            onClipMoved = { _, _ -> },
            onGrabChanged = {},
        )
    }
}
