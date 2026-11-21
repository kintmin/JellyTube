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
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.audio_edit.AudioEditIntent
import com.kintmin.presentation.ui.audio_edit.AudioEditUiState
import com.kintmin.presentation.ui.audio_edit.waveform.TimelineGrab

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
        val hasAudio = data.timeline.hasAudio
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            text = when {
                !hasAudio -> NO_AUDIO_MESSAGE
                // 잡고 있는 동안에는 진행률보다 지금 하는 조작이 중요하다
                grab == null && data.waveformLoadingPercent != null ->
                    "파형 불러오는 중 ${data.waveformLoadingPercent}%"

                else -> data.gestureHint(grab).orEmpty()
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

@Preview
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
