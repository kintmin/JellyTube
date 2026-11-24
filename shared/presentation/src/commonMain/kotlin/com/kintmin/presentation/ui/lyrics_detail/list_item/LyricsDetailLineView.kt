package com.kintmin.presentation.ui.lyrics_detail.list_item

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.extension.toLyricsTimeLabel
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.ui.lyrics_detail.LyricsDetailUiState

@Composable
fun LyricsDetailLineView(
    line: LyricsDetailUiState.SyncedLine,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            modifier = Modifier.alignByBaseline(),
            text = line.timeMs.toLyricsTimeLabel(),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = contentMuted,
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .alignByBaseline()
                .padding(start = 12.dp),
            text = line.text.ifBlank { " " },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Composable
private fun LyricsDetailLineViewPreview() {
    JellyTubeTheme {
        Column {
            LyricsDetailLineView(
                line = LyricsDetailUiState.SyncedLine(
                    timeMs = 62_450L,
                    text = "All my troubles seemed so far away",
                ),
            )
        }
    }
}
