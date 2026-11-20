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
import com.kintmin.presentation.theme.JellyTubeTheme
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
        surfaceModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "피치",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = selectedSemitone.toPitchSemitoneText(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                // 끌 때마다 바뀌는 글자다. 비례폭이면 가운데 정렬이 좌우로 흔들린다.
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
            )
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlaybackStepButton(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "피치 낮추기",
                    onClick = { selectedSemitone.previousPitchSemitone()?.let(onSelectSemitone) },
                )
                PlaybackAdjustSlider(
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
                )
                PlaybackStepButton(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "피치 높이기",
                    onClick = { selectedSemitone.nextPitchSemitone()?.let(onSelectSemitone) },
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaybackPitchOptions.forEach { semitone ->
                    PlaybackOptionChip(
                        modifier = Modifier.weight(1f),
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
