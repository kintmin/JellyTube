package com.kintmin.presentation.ui.lyrics_edit.list_item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.theme.destructiveContent
import com.kintmin.presentation.ui.common.JellyTubeDropdownMenu
import com.kintmin.presentation.ui.common.UnderlineTextField
import com.kintmin.presentation.ui.lyrics_edit.LyricsEditUiState.EditRow

/** timeMs 를 화면 표시용 문자열로 만든다. 1시간 이상이면 시를 포함한다. */
fun formatTimeLabel(timeMs: Long): String {
    val safe = if (timeMs < 0) 0 else timeMs
    val hours = safe / 3_600_000L
    val minutes = (safe % 3_600_000L) / 60_000L
    val seconds = (safe % 60_000L) / 1_000L
    val centiseconds = (safe % 1_000L) / 10L
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    val cc = centiseconds.toString().padStart(2, '0')
    return if (hours > 0) {
        "$hours:$mm:$ss.$cc"
    } else {
        "$mm:$ss.$cc"
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
    var translation by remember(row.id) { mutableStateOf(row.translation) }
    var transliteration by remember(row.id) { mutableStateOf(row.transliteration) }
    var isVariantExpanded by rememberSaveable(row.id) { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp),
    ) {
        // 수정 여부를 색만으로 알리지 않도록 막대와 "수정됨" 글자를 함께 둔다.
        Box(
            modifier = Modifier
                .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
                .width(3.dp)
                .fillMaxHeight()
                .background(
                    color = if (row.isModified) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                    shape = RoundedCornerShape(2.dp),
                ),
        )

        AssistChip(
            modifier = Modifier.padding(start = 8.dp),
            onClick = { onClickTime(row.id) },
            label = {
                Text(
                    text = formatTimeLabel(row.timeMs),
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                )
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        ) {
            UnderlineTextField(
                modifier = Modifier.padding(top = 12.dp),
                label = null,
                value = text,
                onValueChange = {
                    text = it
                    onChangeText(row.id, it)
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = "가사 입력",
                singleLine = false,
                underlineOnlyWhenFocused = true,
            )

            if (showTranslation || showTransliteration) {
                // 눌림 표시를 그리지 않는다. 글자와 화살표가 바뀌는 것으로 충분하고, 리플은 줄 높이만큼 길게 떠 보였다.
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = null,
                            indication = null,
                        ) { isVariantExpanded = !isVariantExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = listOfNotNull(
                            "번역".takeIf { showTranslation },
                            "음차".takeIf { showTransliteration },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge,
                        color = contentMuted,
                    )
                    Icon(
                        imageVector = if (isVariantExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isVariantExpanded) "접기" else "펼치기",
                        tint = contentMuted,
                    )
                }
            }

            if (isVariantExpanded && showTranslation) {
                UnderlineTextField(
                    modifier = Modifier.padding(top = 8.dp),
                    label = "번역",
                    value = translation,
                    onValueChange = {
                        translation = it
                        onChangeTranslation(row.id, it)
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = "번역 입력",
                    singleLine = false,
                    underlineOnlyWhenFocused = true,
                )
            }

            if (isVariantExpanded && showTransliteration) {
                UnderlineTextField(
                    modifier = Modifier.padding(top = 8.dp),
                    label = "음차",
                    value = transliteration,
                    onValueChange = {
                        transliteration = it
                        onChangeTransliteration(row.id, it)
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = "음차 입력",
                    singleLine = false,
                    underlineOnlyWhenFocused = true,
                )
            }

            if (isVariantExpanded) {
                Spacer(Modifier.height(12.dp))
            }

            if (row.isModified) {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = "수정됨",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Box(modifier = Modifier.align(Alignment.Top)) {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "줄 메뉴", tint = contentMuted)
            }
            JellyTubeDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("아래에 줄 추가") },
                    onClick = {
                        menuExpanded = false
                        onAddRowBelow(row.id)
                    },
                )
                DropdownMenuItem(
                    text = { Text("줄 삭제") },
                    colors = MenuDefaults.itemColors(textColor = destructiveContent),
                    onClick = {
                        menuExpanded = false
                        onDeleteRow(row.id)
                    },
                )
            }
        }
    }
}

@Preview
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
