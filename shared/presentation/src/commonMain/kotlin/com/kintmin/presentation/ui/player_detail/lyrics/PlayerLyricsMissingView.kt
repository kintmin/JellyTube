package com.kintmin.presentation.ui.player_detail.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.artworkBackdropFallback
import com.kintmin.presentation.theme.overlayContentMuted
import com.kintmin.presentation.theme.overlayContentSecondary

/**
 * 가사 면의 빈 상태. 가사 찾기로 보내는 것은 [onClickSearch] 뿐이고, 이 뷰 밖의 면을 누르면
 * 아트워크로 돌아간다 — 그래서 여기서는 바탕을 받지 않는다.
 */
@Composable
fun PlayerLyricsMissingView(
    onClickSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "이 곡에는 가사가 없습니다",
            color = overlayContentMuted,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClickSearch)
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = overlayContentSecondary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "가사 찾기",
                color = overlayContentSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** 이 뷰는 아트워크 자리에 그려지므로 프리뷰도 어두운 바탕을 깔아야 색을 볼 수 있다. */
@Preview
@Composable
private fun PlayerLyricsMissingViewPreview() {
    JellyTubeTheme {
        PlayerLyricsMissingView(
            onClickSearch = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(artworkBackdropFallback[1]),
        )
    }
}
