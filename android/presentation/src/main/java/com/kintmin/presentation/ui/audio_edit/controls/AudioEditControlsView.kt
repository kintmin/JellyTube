package com.kintmin.presentation.ui.audio_edit.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss_dot_sss
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.audio_edit.AudioEditIntent
import com.kintmin.presentation.ui.audio_edit.AudioEditUiState
import com.kintmin.presentation.ui.audio_edit.waveform.TimelineGrab
import kotlin.time.Duration.Companion.milliseconds

internal const val NO_AUDIO_MESSAGE = "남은 소리가 없어 저장할 수 없습니다. 조각 경계를 꾹 눌러 끌어 되살려주세요."

internal fun AudioEditUiState.gestureHint(grab: TimelineGrab?): String {
    val grabbedClip = grab?.let { timeline.clips.getOrNull(it.clipIndex) }
    return when {
        grabbedClip == null && selectedClipIndex != null ->
            "양 끝을 꾹 눌러 끌면 길이 조절 · 가운데를 꾹 눌러 끌면 이동"

        grabbedClip == null ->
            "위 눈금을 누르거나 끌면 기준선 이동 · 파형을 누르면 조각 선택 · 두 손가락으로 확대/축소"

        grab.edge == null -> {
            val groupStartMs = timeline.clips[timeline.groupRangeOf(grab.clipIndex).first].positionMs
            "이동 중 · 시작 ${groupStartMs.milliseconds.to_hh_colon_mm_colon_ss_dot_sss()}"
        }

        grab.edge == AudioEditTimeline.Edge.Start ->
            "길이 조절 중 · 시작 ${grabbedClip.positionMs.milliseconds.to_hh_colon_mm_colon_ss_dot_sss()}"

        else ->
            "길이 조절 중 · 끝 ${grabbedClip.positionEndMs.milliseconds.to_hh_colon_mm_colon_ss_dot_sss()}"
    }
}

/**
 * 자르기 버튼이 할 일. 기준선이 맞닿은 두 조각 사이에 있으면 병합으로 바뀐다.
 *
 * 조각 경계에서는 원래 자를 수 없어(양쪽 최소 길이) 두 동작이 겹치는 위치가 없다.
 */
internal fun AudioEditUiState.splitOrMergeIntent(): AudioEditIntent =
    if (canMerge) AudioEditIntent.OnClickMerge else AudioEditIntent.OnClickSplit

/**
 * 파형 아래 조작 영역(세로 화면처럼 높이가 넉넉할 때).
 *
 * 기준선 위치, 자르기·삭제·미리듣기 버튼, 조작 힌트를 위아래로 쌓는다.
 * 높이가 낮을 때는 [AudioEditCompactControlsView]를 쓴다.
 */
@Composable
fun AudioEditControlsView(
    data: AudioEditUiState,
    grab: TimelineGrab?,
    sendIntent: (AudioEditIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "기준선 ${data.baselineMs.milliseconds.to_hh_colon_mm_colon_ss_dot_sss()}",
                fontSize = 15.sp,
            )
            data.waveformLoadingPercent?.let { percent ->
                Text(
                    text = "파형 불러오는 중 $percent%",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val buttonPadding = PaddingValues(horizontal = 8.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                contentPadding = buttonPadding,
                enabled = data.canMerge || data.canSplit,
                onClick = { sendIntent(data.splitOrMergeIntent()) },
            ) {
                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = if (data.canMerge) Icons.Rounded.Merge else Icons.Rounded.ContentCut,
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = if (data.canMerge) "병합" else "자르기",
                    maxLines = 1,
                )
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                contentPadding = buttonPadding,
                enabled = data.canDelete,
                onClick = { sendIntent(AudioEditIntent.OnClickDeleteClip) },
            ) {
                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                )
                Text(modifier = Modifier.padding(start = 4.dp), text = "삭제", maxLines = 1)
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                contentPadding = buttonPadding,
                onClick = { sendIntent(AudioEditIntent.OnClickTogglePreview) },
            ) {
                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = if (data.isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                )
                Text(modifier = Modifier.padding(start = 4.dp), text = "미리듣기", maxLines = 1)
            }
        }

        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = data.gestureHint(grab),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!data.timeline.hasAudio) {
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                text = NO_AUDIO_MESSAGE,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AudioEditControlsViewPreview() {
    JellyTubeTheme {
        AudioEditControlsView(
            data = AudioEditUiState.getMock(),
            grab = null,
            sendIntent = {},
        )
    }
}
