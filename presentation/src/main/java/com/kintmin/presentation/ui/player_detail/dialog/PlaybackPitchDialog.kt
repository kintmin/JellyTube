package com.kintmin.presentation.ui.player_detail.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                text = "피치",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = selectedSemitone.toPitchSemitoneText(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackStepButton(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "피치 낮추기",
                    onClick = {
                        selectedSemitone.previousPitchSemitone()?.let(onSelectSemitone)
                    },
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
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.36f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                PlaybackStepButton(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "피치 높이기",
                    onClick = {
                        selectedSemitone.nextPitchSemitone()?.let(onSelectSemitone)
                    },
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PlaybackPitchOptions.forEach { semitone ->
                    PlaybackPitchOptionChip(
                        semitone = semitone,
                        isSelected = selectedSemitone == semitone,
                        onClick = { onSelectSemitone(semitone) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackStepButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun PlaybackPitchOptionChip(
    semitone: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = if (isSelected) 0.26f else 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = semitone.toPitchSemitoneText(),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private const val MinPlaybackPitchSemitone = -10
private const val MaxPlaybackPitchSemitone = 10
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
