package com.kintmin.presentation.ui.setting.app_log.date_selector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.ui.common.floatingField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLogDateSelectorView(
    dateList: List<String>,
    selectedDate: String?,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 메뉴 폭을 선택과 맞추고, 높이를 선택 아래 남은 공간까지로 줄여 스크롤시킨다.
    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
    ) {
        Row(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .height(48.dp)
                .floatingField()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp),
                imageVector = Icons.Rounded.CalendarToday,
                tint = contentMuted,
                contentDescription = null,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = selectedDate.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                tint = contentMuted,
                contentDescription = null,
            )
        }

        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            shape = MaterialTheme.shapes.medium,
        ) {
            dateList.forEach { date ->
                DropdownMenuItem(
                    text = { Text(text = date) },
                    trailingIcon = if (date == selectedDate) {
                        { Icon(imageVector = Icons.Rounded.Check, contentDescription = "선택됨") }
                    } else {
                        null
                    },
                    onClick = {
                        isExpanded = false
                        onSelectDate(date)
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun AppLogDateSelectorViewPreview() {
    JellyTubeTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            AppLogDateSelectorView(
                dateList = listOf("2026-11-22", "2026-11-21"),
                selectedDate = "2026-11-22",
                onSelectDate = {},
            )
        }
    }
}
