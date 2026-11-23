package com.kintmin.presentation.ui.lyrics_detail.list_item

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.extension.toLyricsTimeLabel
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.lyrics_detail.LyricsDetailUiState

@Composable
fun LyricsDetailLineView(
    line: LyricsDetailUiState.SyncedLine,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                text = line.timeMs.toLyricsTimeLabel(),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            )
        }
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 5.dp, bottom = 5.dp),
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
