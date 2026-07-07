package com.kintmin.presentation.ui.lyrics_search.list_item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.lyrics_search.LyricsSearchUiState

@Composable
fun LyricsSearchItemView(
    item: LyricsSearchUiState.LyricsSearchItem,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(end = 44.dp),
        ) {
            Text(
                text = item.trackName.ifBlank { "제목 없음" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.artistName.isNotBlank()) {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = item.artistName,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.albumName.isNotBlank()) {
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = "앨범명: ${item.albumName}",
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.durationText.isNotBlank()) {
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = "재생시간: ${item.durationText}",
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.plainLyricsPreview.isNotBlank()) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = item.plainLyricsPreview,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (item.syncedLyrics.isNotBlank()) {
            Text(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                text = "SYNC",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LyricsSearchItemViewPreview() {
    JellyTubeTheme {
        LyricsSearchItemView(
            item = LyricsSearchUiState.getMock().results.first().copy(syncedLyrics = "[00:01.00] a"),
            onClick = {},
        )
    }
}
