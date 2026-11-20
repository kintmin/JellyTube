package com.kintmin.presentation.ui.player_detail.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme

/** 줄마다 높이가 변하면 그라데이션이 커졌다 작아진다. 두 줄이 들어가는 높이로 고정한다. */
private val OVERLAY_HEIGHT = 56.dp

/**
 * 지금 가사 한 줄을 아트워크 위에 얹는다.
 *
 * 세로를 따로 쓰지 않으려고 그림 위로 올렸다. 싱크 가사가 아니면 보여 줄 줄이 없으므로
 * 호출하는 쪽이 빈 문자열일 때 아예 그리지 않는다 — 그러면 대부분의 곡에서 그림이 깨끗하다.
 */
@Composable
fun CurrentLyricOverlayView(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(OVERLAY_HEIGHT)
            // 아트워크가 무엇이든 글자가 읽혀야 한다. 아래로 갈수록 짙어지는 막을 깐다.
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.35f to Color.Black.copy(alpha = 0.35f),
                    1f to Color.Black.copy(alpha = 0.72f),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun CurrentLyricOverlayViewPreview() {
    JellyTubeTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFF8A7F6A)),
        ) {
            CurrentLyricOverlayView(
                text = "이 노래의 현재 가사 한 줄이 여기에 표시됩니다",
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
