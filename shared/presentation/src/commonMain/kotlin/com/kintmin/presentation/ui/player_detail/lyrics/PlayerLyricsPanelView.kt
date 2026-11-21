package com.kintmin.presentation.ui.player_detail.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.animation.FollowActiveLyricLine
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.player_detail.PlayerLyricLineUiState

@Composable
fun PlayerLyricsPanelView(
    lines: List<PlayerLyricLineUiState>,
    activeIndex: Int,
    /** 싱크 가사일 때만 준다. */
    onClickLine: ((index: Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    FollowActiveLyricLine(listState = listState, activeIndex = activeIndex)

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeIndex
            // 아트워크에서 뽑은 어두운 그라데이션 위에 그린다. 이 테마의 primary 는 그 위에서
            // 흰 글자보다 어두워, 활성 줄을 그 색으로 알리면 오히려 흐려진다.
            val color = if (isActive) Color.White else Color.White.copy(alpha = 0.55f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .then(if (onClickLine != null) Modifier.clickable { onClickLine(index) } else Modifier)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = line.text.ifBlank { " " },
                    color = color,
                    fontSize = if (isActive) 18.sp else 16.sp,
                    lineHeight = if (isActive) 26.sp else 24.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
                line.transliteration?.let { transliteration ->
                    Text(
                        text = transliteration,
                        color = color.copy(alpha = color.alpha * 0.8f),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                line.translation?.let { translation ->
                    Text(
                        text = translation,
                        color = color.copy(alpha = color.alpha * 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private val previewLines = listOf(
    PlayerLyricLineUiState(text = "첫 줄은 여기에서 시작합니다"),
    PlayerLyricLineUiState(text = "그 다음 줄이 이어집니다"),
    PlayerLyricLineUiState(
        text = "今の歌詞の一行がここに表示されます",
        transliteration = "ima no kashi no ichigyou ga koko ni hyouji saremasu",
        translation = "이 노래의 현재 가사 한 줄이 여기에 표시됩니다",
    ),
    PlayerLyricLineUiState(text = "다음에 올 줄입니다"),
    PlayerLyricLineUiState(text = "그 뒤로도 가사가 이어집니다"),
)

/** 이 패널은 아트워크 자리에 그려지므로 프리뷰도 어두운 바탕을 깔아야 색을 볼 수 있다. */
@Preview
@Composable
private fun PlayerLyricsPanelViewSyncedPreview() {
    JellyTubeTheme {
        PlayerLyricsPanelView(
            lines = previewLines,
            activeIndex = 2,
            onClickLine = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFF1D1C1A)),
        )
    }
}

@Preview
@Composable
private fun PlayerLyricsPanelViewUnsyncedPreview() {
    JellyTubeTheme {
        PlayerLyricsPanelView(
            lines = previewLines,
            activeIndex = -1,
            onClickLine = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFF1D1C1A)),
        )
    }
}
