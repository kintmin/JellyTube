package com.kintmin.presentation.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted

private val TOGGLE_DOT_SIZE = 4.dp

/**
 * `Shuffle` 과 `Repeat` 는 Filled·Rounded 변종이 구분되지 않는다 — 아래 점을 빼면 켜짐이
 * 색으로만 전달된다.
 *
 * 점은 꺼짐일 때도 투명으로 자리를 지킨다 — 빼면 켤 때마다 아이콘이 위로 밀린다.
 */
@Composable
fun PlaybackToggleButton(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    label: String,
    isOn: Boolean,
    iconSize: Dp = 20.dp,
    offColor: Color = contentMuted,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        targetValue = if (isOn) MaterialTheme.colorScheme.tertiary else offColor,
        label = "playbackToggleColor",
    )

    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                modifier = Modifier.size(iconSize),
                imageVector = imageVector,
                contentDescription = if (isOn) "$label 켜짐" else "$label 꺼짐",
                tint = color,
            )
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(TOGGLE_DOT_SIZE)
                    .clip(CircleShape)
                    .background(if (isOn) color else Color.Transparent),
            )
        }
    }
}

@Preview
@Composable
private fun PlaybackToggleButtonPreview() {
    JellyTubeTheme {
        Row {
            PlaybackToggleButton(
                imageVector = Icons.Rounded.Shuffle,
                label = "셔플",
                isOn = true,
                onClick = {},
            )
            PlaybackToggleButton(
                imageVector = Icons.Rounded.Repeat,
                label = "반복",
                isOn = false,
                onClick = {},
            )
        }
    }
}
