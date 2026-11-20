package com.kintmin.presentation.ui.player_detail.dialog

import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.deepSea100
import com.kintmin.presentation.theme.seaBlue80
import com.kintmin.presentation.theme.white

/** 큼직한 라운드가 이 창들을 재생 화면 위에 올려놓은 카드처럼 보이게 한다. */
internal val PlaybackDialogShape = RoundedCornerShape(28.dp)

/**
 * 속도·피치 창의 바탕. 불투명하되 회색이 아니라 앱의 남색이라, 아트워크에서 뽑은 어떤
 * 그라데이션 위에 떠도 따로 놀지 않는다.
 */
internal val PlaybackDialogContainer = deepSea100

@Composable
internal fun PlaybackStepButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        modifier = Modifier.size(44.dp),
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = white.copy(alpha = 0.12f),
            contentColor = white,
        ),
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
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
        shape = RoundedCornerShape(12.dp),
        label = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        },
        // 테두리를 두면 고른 칸과 안 고른 칸의 차이가 테두리에 묻힌다. 채움으로만 가른다.
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = white.copy(alpha = 0.1f),
            labelColor = white.copy(alpha = 0.75f),
            selectedContainerColor = seaBlue80,
            selectedLabelColor = deepSea100,
        ),
    )
}

/** 눈금은 그리지 않는다 — 반음은 21 칸이라 트랙이 점선으로 뒤덮이고, 속도는 눈금이 없다. */
@Composable
internal fun playbackSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = white,
    activeTrackColor = white,
    inactiveTrackColor = white.copy(alpha = 0.22f),
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
)
