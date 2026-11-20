package com.kintmin.presentation.ui.lyrics_viewer.list_item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.kintmin.presentation.theme.JellyTubeTheme

private const val INACTIVE_ALPHA = 0.55f

@Composable
fun LyricsViewerLineView(
    modifier: Modifier = Modifier,
    line: String,
    transliteration: String?,
    translation: String?,
    isActive: Boolean,
    onClick: (() -> Unit)?,
) {
    val color = MaterialTheme.colorScheme.onSurface.let { if (isActive) it else it.copy(alpha = INACTIVE_ALPHA) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = line.ifBlank { " " },
            style = if (isActive) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = color,
            textAlign = TextAlign.Center,
        )
        transliteration?.takeIf { it.isNotBlank() }?.let {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                textAlign = TextAlign.Center,
            )
        }
        translation?.takeIf { it.isNotBlank() }?.let {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun LyricsViewerLineViewPreview() {
    JellyTubeTheme {
        Column {
            LyricsViewerLineView(
                line = "There's a shadow hanging over me",
                transliteration = "데어즈 어 섀도 행잉 오버 미",
                translation = "내 위로 그림자가 드리워져 있어요",
                isActive = true,
                onClick = {},
            )
            LyricsViewerLineView(
                line = "Oh, yesterday came suddenly",
                transliteration = null,
                translation = "아, 어제는 갑자기 찾아왔죠",
                isActive = false,
                onClick = null,
            )
        }
    }
}
