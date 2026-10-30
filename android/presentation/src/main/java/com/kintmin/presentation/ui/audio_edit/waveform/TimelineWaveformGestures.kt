package com.kintmin.presentation.ui.audio_edit.waveform

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import kotlin.math.abs

/** 이 거리 안에서 길게 누르면 조각 경계를 잡는다. */
private val EDGE_TOUCH_RADIUS = 24.dp

/** 손가락을 내린 뒤 어떤 제스처인지 갈린 결과. */
private sealed interface WaveformGesture {
    data object Tap : WaveformGesture
    data object Move : WaveformGesture
    data object LongPress : WaveformGesture
}

/**
 * 파형 영역 제스처.
 *
 * - 짧게 누르기: 조각 선택(빈칸이면 선택 해제)
 * - 한 손가락으로 끌기: 화면 좌우 이동
 * - 두 손가락: 확대/축소와 이동
 * - 길게 누른 뒤 끌기: 경계 근처면 그 조각 끝을 늘리거나 줄이고, 조각 안쪽이면 조각을 통째로 옮긴다
 *
 * 탭·이동·길게 누르기는 서로 배타적이다. 터치 슬롭을 넘으면 이동, 넘기 전에 떼면 탭,
 * 움직이지 않고 길게 누르고 있으면 길게 누르기로 갈린다.
 */
internal suspend fun PointerInputScope.timelineWaveformGestures(
    viewport: TimelineViewportState,
    timeline: () -> AudioEditTimeline,
    onTap: (ms: Long) -> Unit,
    onGrabbed: () -> Unit,
    onEdgeDragged: (index: Int, edge: AudioEditTimeline.Edge, ms: Long) -> Unit,
    onClipMoved: (index: Int, positionMs: Long) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown()

        val gesture = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var decided: WaveformGesture? = null
            while (decided == null) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                decided = when {
                    event.changes.count { it.pressed } >= 2 -> WaveformGesture.Move
                    change == null || !change.pressed -> WaveformGesture.Tap
                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop ->
                        WaveformGesture.Move

                    else -> null
                }
            }
            decided
        } ?: WaveformGesture.LongPress

        when (gesture) {
            WaveformGesture.Tap -> onTap(viewport.msAt(down.position.x))

            WaveformGesture.Move -> {
                while (true) {
                    val event = awaitPointerEvent()
                    val pressedChanges = event.changes.filter { it.pressed }
                    if (pressedChanges.isEmpty()) break

                    if (pressedChanges.size >= 2) {
                        viewport.zoom(zoom = event.calculateZoom(), focusX = event.calculateCentroid().x)
                    }
                    viewport.pan(event.calculatePan().x)
                    event.changes.forEach(PointerInputChange::consume)
                }
            }

            WaveformGesture.LongPress -> {
                val currentTimeline = timeline()
                val grabbedEdge = findGrabbedEdge(
                    viewport = viewport,
                    timeline = currentTimeline,
                    touchX = down.position.x,
                    radiusPx = EDGE_TOUCH_RADIUS.toPx(),
                )
                val downMs = viewport.msAt(down.position.x)
                val grabbedClipIndex = currentTimeline.clipIndexAt(downMs)

                // 경계를 먼저 본다. 경계도 조각도 아닌 빈칸이면 할 일이 없다.
                val onDrag: (Long) -> Unit = when {
                    grabbedEdge != null -> fun(ms: Long) {
                        onEdgeDragged(grabbedEdge.first, grabbedEdge.second, ms)
                    }

                    grabbedClipIndex != null -> {
                        // 누른 지점과 조각 시작 사이 거리를 유지해야 조각이 손가락 밑에서 튀지 않는다
                        val grabOffsetMs = downMs - currentTimeline.clips[grabbedClipIndex].positionMs
                        fun(ms: Long) { onClipMoved(grabbedClipIndex, ms - grabOffsetMs) }
                    }

                    else -> return@awaitEachGesture
                }
                onGrabbed()

                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    if (change.positionChange() != Offset.Zero) {
                        onDrag(viewport.msAt(change.position.x))
                    }
                    change.consume()
                }
            }
        }
    }
}

/**
 * 누른 위치에서 가장 가까운 조각 경계. 반경 안에 없으면 null.
 *
 * 두 조각이 맞닿은 경계는 거리가 같으므로, 손가락이 그 조각 안쪽에 있는 경계를 고른다.
 * 왼쪽 조각 위를 눌렀으면 왼쪽 조각의 끝, 오른쪽 조각 위면 오른쪽 조각의 시작이다.
 */
private fun findGrabbedEdge(
    viewport: TimelineViewportState,
    timeline: AudioEditTimeline,
    touchX: Float,
    radiusPx: Float,
): Pair<Int, AudioEditTimeline.Edge>? {
    data class Candidate(val index: Int, val edge: AudioEditTimeline.Edge, val distance: Float, val isInside: Boolean)

    return timeline.clips
        .flatMapIndexed { index, clip ->
            val startX = viewport.xOf(clip.positionMs)
            val endX = viewport.xOf(clip.positionEndMs)
            listOf(
                Candidate(index, AudioEditTimeline.Edge.Start, abs(touchX - startX), touchX >= startX),
                Candidate(index, AudioEditTimeline.Edge.End, abs(touchX - endX), touchX <= endX),
            )
        }
        .filter { it.distance <= radiusPx }
        .minWithOrNull(compareBy<Candidate> { it.distance }.thenBy { !it.isInside })
        ?.let { it.index to it.edge }
}
