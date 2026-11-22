package com.kintmin.presentation.ui.setting.app_log.date_selector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.ui.common.JellyTubeDropdownMenu
import com.kintmin.presentation.ui.common.floatingField

@Composable
fun AppLogDateSelectorView(
    dateList: List<String>,
    selectedDate: String?,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    // 메뉴는 시스템 바를 뺀 영역 안에서 자리를 잡는다.
    val systemBars = WindowInsets.systemBars
    val windowHeight = LocalWindowInfo.current.containerSize.height - systemBars.getTop(density) - systemBars.getBottom(density)
    var menuMaxHeight by remember { mutableStateOf(MENU_MAX_HEIGHT) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .floatingField()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(role = Role.Button, onClickLabel = "로그 날짜 고르기") { isExpanded = true }
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

        // 폭이 0인 앵커를 선택의 오른쪽 끝에 두면 메뉴가 오른쪽 끝에 맞춰 펼쳐진다.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .onGloballyPositioned { anchor ->
                    // 아래 공간을 넘으면 메뉴가 통째로 위로 밀려 상단 바를 덮는다. 가로 화면에서 그렇다.
                    val spaceBelow = with(density) { (windowHeight - anchor.positionInWindow().y).toDp() } - MENU_EDGE_MARGIN
                    menuMaxHeight = minOf(MENU_MAX_HEIGHT, spaceBelow)
                },
        ) {
            JellyTubeDropdownMenu(
                modifier = Modifier.heightIn(max = menuMaxHeight),
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
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
}

// 다섯 줄 반이 보여 스크롤된다는 게 읽히는 높이. 세로 화면의 3분의 1쯤이다.
private val MENU_MAX_HEIGHT = 280.dp

// M3 메뉴가 창 위아래 가장자리와 띄우는 여백.
private val MENU_EDGE_MARGIN = 48.dp

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
