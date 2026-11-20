package com.kintmin.presentation.ui.player_detail.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.deepSea100
import com.kintmin.presentation.theme.seaBlue80

/**
 * A-B 구간반복의 세 상태를 형태로 가른다 — 아무것도 안 잡힘 / A 만 / 둘 다.
 * 구간이 어디인지는 재생바의 핀과 밴드가 말한다.
 */
@Composable
fun RepeatRangeChipView(
    isStartSelected: Boolean,
    isEndSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .border(
                width = 1.5.dp,
                color = if (isStartSelected) seaBlue80 else Color.White.copy(alpha = 0.45f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = when {
                    isStartSelected && isEndSelected -> "구간반복, 구간을 반복하고 있습니다"
                    isStartSelected -> "구간반복, 시작 지점만 잡혔습니다"
                    else -> "구간반복, 꺼짐"
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RepeatRangeSegment(label = "A", isFilled = isStartSelected)
        RepeatRangeSegment(label = "B", isFilled = isEndSelected)
    }
}

@Composable
private fun RepeatRangeSegment(
    label: String,
    isFilled: Boolean,
) {
    // 켜진 쪽이 밝아야 한다. 이 테마의 어두운 파랑으로 켜짐을 알리면 아트워크 그라데이션
    // 위에서 꺼진 흰 글자보다 흐려져 방향이 뒤집혀 보인다.
    Text(
        modifier = Modifier
            .background(if (isFilled) seaBlue80 else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        text = label,
        color = if (isFilled) deepSea100 else Color.White.copy(alpha = 0.6f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Preview
@Composable
private fun RepeatRangeChipViewPreview() {
    JellyTubeTheme {
        Row(
            modifier = Modifier
                .background(Color(0xFF1D1C1A))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RepeatRangeChipView(isStartSelected = false, isEndSelected = false, onClick = {})
            RepeatRangeChipView(isStartSelected = true, isEndSelected = false, onClick = {})
            RepeatRangeChipView(isStartSelected = true, isEndSelected = true, onClick = {})
        }
    }
}
