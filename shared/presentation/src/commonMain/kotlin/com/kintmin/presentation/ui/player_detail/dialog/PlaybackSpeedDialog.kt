package com.kintmin.presentation.ui.player_detail.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
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
import com.kintmin.presentation.theme.playerOverlay
import com.kintmin.presentation.theme.white
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
        shape = PlaybackDialogShape,
        containerColor = playerOverlay,
        surfaceModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "재생 속도",
                color = white.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = selectedSpeed.toPlaybackSpeedText(),
                color = white,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackStepButton(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "재생 속도 낮추기",
                    onClick = { selectedSpeed.previousPlaybackSpeed()?.let(onSelectSpeed) },
                )
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
                    colors = playbackSliderColors(showTicks = false),
                )
                Spacer(modifier = Modifier.width(8.dp))
                PlaybackStepButton(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "재생 속도 높이기",
                    onClick = { selectedSpeed.nextPlaybackSpeed()?.let(onSelectSpeed) },
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PlaybackSpeedOptions.forEach { speed ->
                    PlaybackOptionChip(
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
