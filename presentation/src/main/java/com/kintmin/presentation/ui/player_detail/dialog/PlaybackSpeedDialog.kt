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
import androidx.compose.material3.MaterialTheme
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
import java.util.Locale
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
        containerColor = Color.Black.copy(alpha = 0.8f),
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
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = selectedSpeed.toPlaybackSpeedText(),
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
                    contentDescription = "재생 속도 낮추기",
                    onClick = {
                        selectedSpeed.previousPlaybackSpeed()?.let(onSelectSpeed)
                    },
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
                    contentDescription = "재생 속도 높이기",
                    onClick = {
                        selectedSpeed.nextPlaybackSpeed()?.let(onSelectSpeed)
                    },
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PlaybackSpeedOptions.forEach { speed ->
                    PlaybackSpeedOptionChip(
                        speed = speed,
                        isSelected = selectedSpeed == speed,
                        onClick = { onSelectSpeed(speed) },
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
private fun PlaybackSpeedOptionChip(
    speed: Float,
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
            text = speed.formatPlaybackSpeed(),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private const val MinPlaybackSpeed = 0.25f
private const val MaxPlaybackSpeed = 3.0f
private const val PlaybackSpeedStep = 0.01f

private val PlaybackSpeedOptions = listOf(1.0f, 1.25f, 1.5f, 2.0f, 3.0f)

fun Float.toPlaybackSpeedText(): String = "x${formatPlaybackSpeed()}"

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
        String.format(Locale.ROOT, "%.1f", this)
    } else {
        String.format(Locale.ROOT, "%.2f", this).trimEnd('0')
    }
}
