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
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.playerOverlay
import com.kintmin.presentation.theme.white
import com.kintmin.presentation.ui.common.JellyTubeDialog
import kotlin.math.roundToInt

@Composable
fun PlaybackPitchDialog(
    showDialog: Boolean,
    selectedSemitone: Int,
    onDismiss: () -> Unit,
    onSelectSemitone: (Int) -> Unit,
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
                text = "피치",
                color = white.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = selectedSemitone.toPitchSemitoneText(),
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
                    contentDescription = "피치 낮추기",
                    onClick = { selectedSemitone.previousPitchSemitone()?.let(onSelectSemitone) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    modifier = Modifier.weight(1f),
                    value = selectedSemitone.toFloat(),
                    onValueChange = { value ->
                        val semitone =
                            value.roundToInt().coerceIn(MinPlaybackPitchSemitone, MaxPlaybackPitchSemitone)
                        if (semitone != selectedSemitone) {
                            onSelectSemitone(semitone)
                        }
                    },
                    valueRange = MinPlaybackPitchSemitone.toFloat()..MaxPlaybackPitchSemitone.toFloat(),
                    steps = MaxPlaybackPitchSemitone - MinPlaybackPitchSemitone - 1,
                    // 반음 단위라 눈금이 21 개다. 그려 두면 트랙이 점선으로 뒤덮인다.
                    colors = playbackSliderColors(showTicks = false),
                )
                Spacer(modifier = Modifier.width(8.dp))
                PlaybackStepButton(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "피치 높이기",
                    onClick = { selectedSemitone.nextPitchSemitone()?.let(onSelectSemitone) },
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PlaybackPitchOptions.forEach { semitone ->
                    PlaybackOptionChip(
                        label = semitone.toPitchSemitoneText(),
                        isSelected = selectedSemitone == semitone,
                        onClick = { onSelectSemitone(semitone) },
                    )
                }
            }
        }
    }
}

private const val MinPlaybackPitchSemitone = PlaybackRange.MIN_PITCH_SEMITONE
private const val MaxPlaybackPitchSemitone = PlaybackRange.MAX_PITCH_SEMITONE
private const val PlaybackPitchStep = 1

private val PlaybackPitchOptions = listOf(-4, -2, 0, 2, 4)

fun Int.toPitchSemitoneText(): String {
    return when {
        this > 0 -> "+$this"
        else -> toString()
    }
}

private fun Int.previousPitchSemitone(): Int? {
    return (this - PlaybackPitchStep).takeIf { it >= MinPlaybackPitchSemitone }
}

private fun Int.nextPitchSemitone(): Int? {
    return (this + PlaybackPitchStep).takeIf { it <= MaxPlaybackPitchSemitone }
}

@Preview
@Composable
private fun PlaybackPitchDialogPreview() {
    JellyTubeTheme {
        PlaybackPitchDialog(
            showDialog = true,
            selectedSemitone = 2,
            onDismiss = {},
            onSelectSemitone = {},
        )
    }
}
