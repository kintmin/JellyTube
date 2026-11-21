package com.kintmin.presentation.ui.player_detail.control

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.artworkBackdropFallback
import com.kintmin.presentation.theme.overlayContentSecondary

/**
 * 양 끝에 서는 값의 폭. **좌우가 같아야 가운데 것이 화면 중앙에 온다.**
 * 가장 넓은 값(`x1.25`)이 들어가는 크기다.
 */
val PLAYBACK_VALUE_WIDTH = 84.dp

/**
 * 피치·속도처럼 지금 값을 보여주고 누르면 창이 열리는 자리.
 *
 * 배경을 두지 않는다 — 대신 앞에 붙은 아이콘이 무엇의 값인지와 **누를 수 있다**를 같이 말한다.
 */
@Composable
fun PlaybackValueView(
    imageVector: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = overlayContentSecondary,
        )
        Text(
            text = label,
            color = overlayContentSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            // 숫자 폭을 같게 해 값이 바뀔 때 폭이 흔들리지 않게 한다.
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
        )
    }
}

@Preview
@Composable
private fun PlaybackValueViewPreview() {
    JellyTubeTheme {
        Row(
            modifier = Modifier
                .background(artworkBackdropFallback[1])
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlaybackValueView(
                imageVector = Icons.Rounded.MusicNote,
                label = "+2",
                contentDescription = "피치",
                onClick = {},
            )
            PlaybackValueView(
                imageVector = Icons.Rounded.Speed,
                label = "x1.25",
                contentDescription = "재생 속도",
                onClick = {},
            )
        }
    }
}
