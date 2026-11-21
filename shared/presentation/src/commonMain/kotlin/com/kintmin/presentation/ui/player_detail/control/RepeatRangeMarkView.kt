package com.kintmin.presentation.ui.player_detail.control

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.artworkBackdropFallback
import com.kintmin.presentation.theme.overlayContentFaint
import com.kintmin.presentation.theme.overlayContentMuted

private val LINK_WIDTH = 9.dp
private val LINK_HEIGHT = 2.dp

/**
 * A-B 구간반복의 세 상태를 두 글자와 그 사이를 잇는 선으로 말한다 — 아무것도 안 잡힘 / A 만 /
 * 둘 다. **선은 구간이 완성됐을 때만 밝아진다.**
 *
 * 테두리도 배경도 두지 않는다. 옆에 선 잠들기·가사가 맨 아이콘이라, 혼자 상자를 두르면 무게가
 * 어긋난다. 구간이 어디인지는 재생바의 핀과 밴드가 말한다.
 */
@Composable
fun RepeatRangeMarkView(
    isStartSelected: Boolean,
    isEndSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLinked = isStartSelected && isEndSelected
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = when {
                    isLinked -> "구간반복, 구간을 반복하고 있습니다"
                    isStartSelected -> "구간반복, 시작 지점만 잡혔습니다"
                    else -> "구간반복, 꺼짐"
                }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        RangeLetter(letter = "A", isOn = isStartSelected)
        Box(
            modifier = Modifier
                .width(LINK_WIDTH)
                .height(LINK_HEIGHT)
                .clip(RoundedCornerShape(LINK_HEIGHT / 2))
                .background(if (isLinked) MaterialTheme.colorScheme.tertiary else overlayContentFaint),
        )
        RangeLetter(letter = "B", isOn = isEndSelected)
    }
}

@Composable
private fun RangeLetter(letter: String, isOn: Boolean) {
    Text(
        text = letter,
        color = if (isOn) MaterialTheme.colorScheme.tertiary else overlayContentMuted,
        fontSize = 14.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Preview
@Composable
private fun RepeatRangeMarkViewPreview() {
    JellyTubeTheme {
        Row(
            modifier = Modifier
                .background(artworkBackdropFallback[1])
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RepeatRangeMarkView(isStartSelected = false, isEndSelected = false, onClick = {})
            RepeatRangeMarkView(isStartSelected = true, isEndSelected = false, onClick = {})
            RepeatRangeMarkView(isStartSelected = true, isEndSelected = true, onClick = {})
            // 옆에 설 아이콘과 무게가 맞는지 같이 본다.
            Box(modifier = Modifier.size(24.dp).background(overlayContentMuted))
        }
    }
}
