package com.kintmin.presentation.ui.player_detail.dialog

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.deepSea100
import com.kintmin.presentation.theme.seaBlue80
import com.kintmin.presentation.theme.white

/**
 * 속도·피치 창이 같이 쓰는 부품과 색.
 *
 * 이 창들은 테마의 `surface` 가 아니라 반투명한 `playerOverlay` 위에 그려진다. 그래서 스톡
 * 부품의 기본색(어두운 `primary`)을 그대로 쓰면 바탕에 묻힌다 — 밝은 쪽으로 세워 준다.
 */
internal val PlaybackDialogShape = RoundedCornerShape(24.dp)

@Composable
internal fun PlaybackStepButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = white.copy(alpha = 0.16f),
            contentColor = white,
        ),
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

@Composable
internal fun PlaybackOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text = label, fontSize = 12.sp) },
        // 테두리를 두면 고른 칸과 안 고른 칸의 차이가 테두리에 묻힌다. 채움으로만 가른다.
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = white.copy(alpha = 0.12f),
            labelColor = white.copy(alpha = 0.8f),
            selectedContainerColor = seaBlue80,
            selectedLabelColor = deepSea100,
        ),
    )
}

@Composable
internal fun playbackSliderColors(showTicks: Boolean): SliderColors = SliderDefaults.colors(
    thumbColor = white,
    activeTrackColor = white,
    inactiveTrackColor = white.copy(alpha = 0.3f),
    activeTickColor = if (showTicks) white.copy(alpha = 0.5f) else Color.Transparent,
    inactiveTickColor = if (showTicks) white.copy(alpha = 0.3f) else Color.Transparent,
)
