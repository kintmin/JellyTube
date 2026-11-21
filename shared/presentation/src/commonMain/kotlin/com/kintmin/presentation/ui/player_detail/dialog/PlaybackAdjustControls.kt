package com.kintmin.presentation.ui.player_detail.dialog

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.selectedFill
import com.kintmin.presentation.theme.subtleFill

@Composable
internal fun PlaybackStepButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        modifier = Modifier.size(36.dp),
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = selectedFill,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

/**
 * 프리셋 칸. 라벨 길이가 제각각이라 호출부가 폭을 똑같이 잡아 준다 — 들쭉날쭉한 칩이 늘어서면
 * 줄이 지저분해진다.
 */
@Composable
internal fun PlaybackOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        modifier = modifier,
        selected = isSelected,
        onClick = onClick,
        shape = CircleShape,
        label = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        },
        // 테두리를 두면 고른 칸과 안 고른 칸의 차이가 테두리에 묻힌다. 채움으로만 가른다.
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = subtleFill,
            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

/**
 * 기본 슬라이더는 트랙이 16dp 라 창 안에서 너무 무겁다. 트랙·thumb 를 줄이고, 눈금과 끝점은
 * 그리지 않는다 — 반음은 21 칸이라 트랙이 점선으로 뒤덮인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaybackAdjustSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.onSurface,
        activeTrackColor = MaterialTheme.colorScheme.onSurface,
        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
    )
    Slider(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        colors = colors,
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                colors = colors,
                thumbSize = DpSize(4.dp, 24.dp),
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                modifier = Modifier.height(6.dp),
                sliderState = sliderState,
                colors = colors,
                drawStopIndicator = null,
                drawTick = { _, _ -> },
                thumbTrackGapSize = 4.dp,
            )
        },
    )
}
