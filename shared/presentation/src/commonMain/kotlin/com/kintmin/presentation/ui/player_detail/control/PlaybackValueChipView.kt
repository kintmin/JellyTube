package com.kintmin.presentation.ui.player_detail.control

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme

/** 재생 속도·피치처럼 현재 값을 글자로 보여주고 누르면 창이 열리는 칩. */
@Composable
fun PlaybackValueChipView(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            // 숫자 폭을 같게 해 값이 바뀔 때 칩 폭이 흔들리지 않게 한다.
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
        )
    }
}

@Preview
@Composable
private fun PlaybackValueChipViewPreview() {
    JellyTubeTheme {
        Row(
            modifier = Modifier
                .background(Color(0xFF1D1C1A))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlaybackValueChipView(
                label = "x1.25",
                onClick = {},
                modifier = Modifier.widthIn(min = 64.dp),
            )
            PlaybackValueChipView(
                label = "+2",
                onClick = {},
                modifier = Modifier.widthIn(min = 52.dp),
            )
        }
    }
}
