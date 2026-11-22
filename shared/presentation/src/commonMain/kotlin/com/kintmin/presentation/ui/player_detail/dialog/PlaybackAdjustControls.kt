package com.kintmin.presentation.ui.player_detail.dialog

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.selectedFill
import com.kintmin.presentation.theme.subtleFill

@Composable
internal fun PlaybackDialogColumn(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}

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
 * 프리셋 칸을 같은 폭으로 늘어놓는다. 라벨 길이가 제각각이라 들쭉날쭉하게 두면 줄이 지저분해지고,
 * 좁은 창에서 폭을 억지로 나누면 라벨이 세로로 꺾인다. 그래서 가장 긴 라벨이 들어가는 폭을 칸 폭으로
 * 삼아 한 줄에 다 들어가면 남는 폭을 나눠 채우고, 안 들어가면 줄 수를 늘려 줄마다 고르게 나눈다.
 */
@Composable
internal fun <T> PlaybackOptionChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            options.forEach { option ->
                PlaybackOptionChip(
                    label = label(option),
                    isSelected = option == selected,
                    onClick = { onSelect(option) },
                )
            }
        },
    ) { measurables, constraints ->
        val gap = 8.dp.roundToPx()
        val count = measurables.size
        val cellMinWidth = measurables.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        val fitPerRow = ((constraints.maxWidth + gap) / (cellMinWidth + gap)).coerceIn(1, count)
        val rows = (count + fitPerRow - 1) / fitPerRow
        val perRow = (count + rows - 1) / rows
        val cellWidth = (constraints.maxWidth - gap * (perRow - 1)) / perRow
        val placeables = measurables.map { it.measure(Constraints.fixedWidth(cellWidth)) }
        val rowHeight = placeables.maxOf { it.height }
        layout(constraints.maxWidth, rowHeight * rows + gap * (rows - 1)) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = (index % perRow) * (cellWidth + gap),
                    y = (index / perRow) * (rowHeight + gap),
                )
            }
        }
    }
}

@Composable
private fun PlaybackOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
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

@Preview
@Composable
private fun PlaybackOptionChipRowPreview() {
    JellyTubeTheme {
        PlaybackOptionChipRow(
            options = listOf(1.0f, 1.25f, 1.5f, 2.0f, 3.0f),
            selected = 1.25f,
            label = { "x$it" },
            onSelect = {},
        )
    }
}

@Preview
@Composable
private fun PlaybackOptionChipRowNarrowPreview() {
    JellyTubeTheme {
        Box(modifier = Modifier.width(240.dp)) {
            PlaybackOptionChipRow(
                options = listOf(15, 30, 45, 60, 90),
                selected = 30,
                label = { "${it}분" },
                onSelect = {},
            )
        }
    }
}

@Preview
@Composable
private fun PlaybackDialogColumnPreview() {
    JellyTubeTheme {
        PlaybackDialogColumn(title = "재생 속도") {
            Text(text = "1.0x", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
