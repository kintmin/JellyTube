package com.kintmin.presentation.ui.lyrics_edit.list_item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Reorder
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
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
    draggingItemId: Int?,
    onClickTime: (rowId: Int) -> Unit,
    onChangeText: (rowId: Int, text: String) -> Unit,
    onAddRowBelow: (rowId: Int) -> Unit,
    onDeleteRow: (rowId: Int) -> Unit,
    onDragStart: (Offset, Int) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    var text by remember(row.id) { mutableStateOf(row.text) }

    val isDragging = row.id == draggingItemId
    val background = when {
        isDragging -> MaterialTheme.colorScheme.surfaceVariant
        row.isModified -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
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

            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onChangeText(row.id, it)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            IconButton(
                modifier = Modifier.pointerInput(row.id) {
                    detectDragGestures(
                        onDragStart = { onDragStart(it, row.id) },
                        onDrag = { change, dragAmount -> onDrag(change, dragAmount) },
                        onDragEnd = { onDragEnd() },
                    )
                },
                onClick = {},
            ) {
                Icon(
                    imageVector = Icons.Rounded.Reorder,
                    contentDescription = "순서 변경 핸들",
                )
            }
        }

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
                row = EditRow(id = 0, timeMs = 62_450L, text = "당신의 창 가까이 보낼게요", isModified = false),
                draggingItemId = null,
                onClickTime = {},
                onChangeText = { _, _ -> },
                onAddRowBelow = {},
                onDeleteRow = {},
                onDragStart = { _, _ -> },
                onDrag = { _, _ -> },
                onDragEnd = {},
            )
            LyricsEditItemView(
                modifier = Modifier,
                row = EditRow(id = 1, timeMs = 71_000L, text = "음 사랑한다는 말이에요", isModified = true),
                draggingItemId = null,
                onClickTime = {},
                onChangeText = { _, _ -> },
                onAddRowBelow = {},
                onDeleteRow = {},
                onDragStart = { _, _ -> },
                onDrag = { _, _ -> },
                onDragEnd = {},
            )
        }
    }
}
