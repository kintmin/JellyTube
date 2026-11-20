package com.kintmin.presentation.ui.player_detail.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
    JellyTubeDialog(
        showDialog = showDialog,
        onDismiss = onDismiss,
        surfaceModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "재생 속도",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = selectedSpeed.toPlaybackSpeedText(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = { selectedSpeed.previousPlaybackSpeed()?.let(onSelectSpeed) },
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "재생 속도 낮추기",
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
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
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = { selectedSpeed.nextPlaybackSpeed()?.let(onSelectSpeed) },
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "재생 속도 높이기",
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PlaybackSpeedOptions.forEach { speed ->
                    FilterChip(
                        selected = selectedSpeed == speed,
                        onClick = { onSelectSpeed(speed) },
                        label = { Text(text = speed.formatPlaybackSpeed(), fontSize = 12.sp) },
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
