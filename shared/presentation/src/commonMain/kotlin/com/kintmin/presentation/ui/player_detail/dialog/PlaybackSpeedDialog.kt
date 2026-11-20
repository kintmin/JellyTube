package com.kintmin.presentation.ui.player_detail.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.domain.audio_play_setting.PlaybackRange
import com.kintmin.presentation.extension.toFixed
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.JellyTubeDialog
import kotlin.math.roundToInt

@Composable
fun PlaybackSpeedDialog(
    showDialog: Boolean,
    selectedSpeed: Float,
    onDismiss: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
) {
    JellyTubeDialog(showDialog = showDialog, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "재생 속도",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = selectedSpeed.toPlaybackSpeedText(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                // 끌 때마다 바뀌는 글자다. 비례폭이면 가운데 정렬이 좌우로 흔들린다.
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlaybackStepButton(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "재생 속도 낮추기",
                    onClick = { selectedSpeed.previousPlaybackSpeed()?.let(onSelectSpeed) },
                )
                PlaybackAdjustSlider(
                    modifier = Modifier.weight(1f),
                    value = selectedSpeed.coerceIn(MinPlaybackSpeed, MaxPlaybackSpeed),
                    onValueChange = { value ->
                        val speed = value.roundToPlaybackSpeedStep()
                        if (speed != selectedSpeed) {
                            onSelectSpeed(speed)
                        }
                    },
                    valueRange = MinPlaybackSpeed..MaxPlaybackSpeed,
                )
                PlaybackStepButton(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "재생 속도 높이기",
                    onClick = { selectedSpeed.nextPlaybackSpeed()?.let(onSelectSpeed) },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaybackSpeedOptions.forEach { speed ->
                    PlaybackOptionChip(
                        modifier = Modifier.weight(1f),
                        label = speed.formatPlaybackSpeed(),
                        isSelected = selectedSpeed == speed,
                        onClick = { onSelectSpeed(speed) },
                    )
                }
            }
        }
    }
}

private const val MinPlaybackSpeed = PlaybackRange.MIN_SPEED
private const val MaxPlaybackSpeed = PlaybackRange.MAX_SPEED
private const val PlaybackSpeedStep = 0.01f

private val PlaybackSpeedOptions = listOf(1.0f, 1.25f, 1.5f, 2.0f, 3.0f)

fun Float.toPlaybackSpeedText(): String = "x${toFixed(2)}"

private fun Float.previousPlaybackSpeed(): Float? {
    return (this - PlaybackSpeedStep)
        .takeIf { it >= MinPlaybackSpeed }
        ?.roundToPlaybackSpeedStep()
}

private fun Float.nextPlaybackSpeed(): Float? {
    return (this + PlaybackSpeedStep)
        .takeIf { it <= MaxPlaybackSpeed }
        ?.roundToPlaybackSpeedStep()
}

private fun Float.roundToPlaybackSpeedStep(): Float {
    return (this / PlaybackSpeedStep).roundToInt()
        .times(PlaybackSpeedStep)
        .coerceIn(MinPlaybackSpeed, MaxPlaybackSpeed)
}

private fun Float.formatPlaybackSpeed(): String {
    return if (this % 1.0f == 0f) {
        toFixed(1)
    } else {
        toFixed(2).trimEnd('0')
    }
}

@Preview
@Composable
private fun PlaybackSpeedDialogPreview() {
    JellyTubeTheme {
        PlaybackSpeedDialog(
            showDialog = true,
            selectedSpeed = 1.25f,
            onDismiss = {},
            onSelectSpeed = {},
        )
    }
}
