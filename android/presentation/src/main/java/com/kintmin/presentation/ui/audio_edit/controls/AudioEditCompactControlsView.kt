package com.kintmin.presentation.ui.audio_edit.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss_dot_sss
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.audio_edit.AudioEditIntent
import com.kintmin.presentation.ui.audio_edit.AudioEditUiState
import com.kintmin.presentation.ui.audio_edit.waveform.TimelineGrab
import kotlin.time.Duration.Companion.milliseconds

/**
 * 파형 아래 조작 영역(가로 화면처럼 높이가 낮을 때).
 *
 * 파형에 높이를 최대한 남기려고 기준선 위치, 한 줄 힌트, 아이콘 버튼을 한 줄에 모은다.
 * 소리가 남지 않았으면 힌트 자리에 경고를 띄운다.
 */
@Composable
fun AudioEditCompactControlsView(
    data: AudioEditUiState,
    grab: TimelineGrab?,
    sendIntent: (AudioEditIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "기준선 ${data.baselineMs.milliseconds.to_hh_colon_mm_colon_ss_dot_sss()}",
            fontSize = 14.sp,
        )

        val hasAudio = data.timeline.hasAudio
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            text = when {
                !hasAudio -> NO_AUDIO_MESSAGE
                // 잡고 있는 동안에는 진행률보다 지금 하는 조작이 중요하다
                grab == null && data.waveformLoadingPercent != null ->
                    "파형 불러오는 중 ${data.waveformLoadingPercent}%"

                else -> data.gestureHint(grab)
            },
            fontSize = 12.sp,
            color = if (hasAudio) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        IconButton(
            enabled = data.canMerge || data.canSplit,
            onClick = { sendIntent(data.splitOrMergeIntent()) },
        ) {
            if (data.canMerge) {
                Icon(imageVector = Icons.Rounded.Merge, contentDescription = "병합")
            } else {
                Icon(imageVector = Icons.Rounded.ContentCut, contentDescription = "자르기")
            }
        }
        IconButton(
            enabled = data.canDelete,
            onClick = { sendIntent(AudioEditIntent.OnClickDeleteClip) },
        ) {
            Icon(imageVector = Icons.Rounded.Delete, contentDescription = "삭제")
        }
        IconButton(onClick = { sendIntent(AudioEditIntent.OnClickTogglePreview) }) {
            Icon(
                imageVector = if (data.isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "미리듣기",
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 720)
@Composable
fun AudioEditCompactControlsViewPreview() {
    JellyTubeTheme {
        AudioEditCompactControlsView(
            data = AudioEditUiState.getMock(),
            grab = TimelineGrab(clipIndex = 1, edge = null),
            sendIntent = {},
        )
    }
}
