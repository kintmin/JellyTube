package com.kintmin.presentation.ui.lyrics_edit.list_item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.lyrics_edit.LyricsEditUiState.EditRow
import java.util.Locale

/** timeMs 를 화면 표시용 문자열로 만든다. 1시간 이상이면 시를 포함한다. */
fun formatTimeLabel(timeMs: Long): String {
    val safe = if (timeMs < 0) 0 else timeMs
    val hours = safe / 3_600_000L
    val minutes = (safe % 3_600_000L) / 60_000L
    val seconds = (safe % 60_000L) / 1_000L
    val centiseconds = (safe % 1_000L) / 10L
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds)
    } else {
        String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centiseconds)
    }
}

@Composable
fun LyricsEditItemView(
    modifier: Modifier,
    row: EditRow,
    showTranslation: Boolean,
    showTransliteration: Boolean,
    onClickTime: (rowId: Int) -> Unit,
    onChangeText: (rowId: Int, text: String) -> Unit,
    onChangeTranslation: (rowId: Int, text: String) -> Unit,
    onChangeTransliteration: (rowId: Int, text: String) -> Unit,
    onAddRowBelow: (rowId: Int) -> Unit,
    onDeleteRow: (rowId: Int) -> Unit,
) {
    var text by remember(row.id) { mutableStateOf(row.text) }

    val background = if (row.isModified) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        // x  시간설정
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDeleteRow(row.id) }) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "이 줄 삭제",
                    tint = MaterialTheme.colorScheme.error,
                )
            }

            Text(
                text = formatTimeLabel(row.timeMs),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClickTime(row.id) }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
            )
        }

        // 원본
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onChangeText(row.id, it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            label = { Text("원본", fontSize = 11.sp) },
        )

        // 번역
        if (showTranslation) {
            var translation by remember(row.id) { mutableStateOf(row.translation) }
            OutlinedTextField(
                value = translation,
                onValueChange = {
                    translation = it
                    onChangeTranslation(row.id, it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                label = { Text("번역", fontSize = 11.sp) },
            )
        }

        // 음차
        if (showTransliteration) {
            var transliteration by remember(row.id) { mutableStateOf(row.transliteration) }
            OutlinedTextField(
                value = transliteration,
                onValueChange = {
                    transliteration = it
                    onChangeTransliteration(row.id, it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                label = { Text("음차", fontSize = 11.sp) },
            )
        }

        // +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(onClick = { onAddRowBelow(row.id) }) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "이 밑에 가사 추가",
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LyricsEditItemViewPreview() {
    JellyTubeTheme {
        Column {
            LyricsEditItemView(
                modifier = Modifier,
                row = EditRow(
                    id = 0,
                    timeMs = 62_450L,
                    text = "All my troubles seemed so far away",
                    translation = "내 모든 고민이 아주 멀게 느껴졌죠",
                    transliteration = "올 마이 트러블스 심드 소 파 어웨이",
                    isModified = false,
                ),
                showTranslation = true,
                showTransliteration = true,
                onClickTime = {},
                onChangeText = { _, _ -> },
                onChangeTranslation = { _, _ -> },
                onChangeTransliteration = { _, _ -> },
                onAddRowBelow = {},
                onDeleteRow = {},
            )
            LyricsEditItemView(
                modifier = Modifier,
                row = EditRow(id = 1, timeMs = 71_000L, text = "음 사랑한다는 말이에요", isModified = true),
                showTranslation = false,
                showTransliteration = false,
                onClickTime = {},
                onChangeText = { _, _ -> },
                onChangeTranslation = { _, _ -> },
                onChangeTransliteration = { _, _ -> },
                onAddRowBelow = {},
                onDeleteRow = {},
            )
        }
    }
}
