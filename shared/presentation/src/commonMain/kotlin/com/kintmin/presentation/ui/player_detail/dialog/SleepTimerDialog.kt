package com.kintmin.presentation.ui.player_detail.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.theme.JellyTubeButtonColors
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.subtleFill
import com.kintmin.presentation.ui.common.JellyTubeDialog
import com.kintmin.presentation.ui.common.wheel_picker.NumberWheelPicker
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun SleepTimerDialog(
    showDialog: Boolean,
    minutes: Int,
    remaining: Duration?,
    announcementName: String?,
    onDismiss: () -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onClickAnnouncement: () -> Unit,
    onStart: () -> Unit,
    onCancelTimer: () -> Unit,
) {
    JellyTubeDialog(showDialog = showDialog, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "잠들기 타이머",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            // 켜짐을 색으로 가르지 않는다 — 이 테마의 primary 는 surface 와 대비가 2.6:1 뿐이다.
            Text(
                text = remaining?.let { "${it.to_hh_colon_mm_colon_ss()} 뒤에 멈춥니다" }
                    ?: "정한 시간이 지나면 천천히 작아지며 멈춥니다",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = if (remaining == null) FontWeight.Normal else FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SleepTimerMinuteOptions.forEach { option ->
                    PlaybackOptionChip(
                        modifier = Modifier.weight(1f),
                        label = "${option}분",
                        isSelected = minutes == option,
                        onClick = { onSelectMinutes(option) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            NumberWheelPicker(
                values = SleepTimerMinuteRange,
                selectedValue = minutes,
                onValueChange = onSelectMinutes,
                label = "분",
                formatter = { it.toString() },
                // 휠은 끌어서 쓰므로 대화상자 폭을 다 쓴다. 양옆 여백은 바깥 Column 의 20dp 가 준다.
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            // 칩·+/- 버튼처럼 채움으로 가른다. 구분선을 두면 이 대화상자만 설정 화면 줄처럼 읽힌다.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    // clickable 보다 먼저 잘라야 눌림 표시도 둥글게 번진다.
                    .clip(RoundedCornerShape(12.dp))
                    .background(subtleFill)
                    .clickable(onClick = onClickAnnouncement)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "마무리 음원",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = announcementName ?: "없음",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Icon(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(18.dp),
                    imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                    contentDescription = "마무리 음원 고르기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onStart,
            ) {
                Text(text = if (remaining == null) "${minutes}분 뒤에 멈추기" else "${minutes}분으로 다시 맞추기")
            }
            if (remaining != null) {
                OutlinedButton(
                    colors = JellyTubeButtonColors.secondary(),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    onClick = onCancelTimer,
                ) {
                    Text(text = "타이머 끄기")
                }
            }
        }
    }
}

private val SleepTimerMinuteOptions = listOf(15, 30, 45, 60, 90)

private val SleepTimerMinuteRange = (1..120).toList()

@Preview
@Composable
private fun SleepTimerDialogPreview() {
    JellyTubeTheme {
        SleepTimerDialog(
            showDialog = true,
            minutes = 30,
            remaining = null,
            announcementName = null,
            onDismiss = {},
            onSelectMinutes = {},
            onClickAnnouncement = {},
            onStart = {},
            onCancelTimer = {},
        )
    }
}

@Preview
@Composable
private fun SleepTimerDialogRunningPreview() {
    JellyTubeTheme {
        SleepTimerDialog(
            showDialog = true,
            minutes = 30,
            remaining = 23.minutes + 12.seconds,
            announcementName = "잘 자요",
            onDismiss = {},
            onSelectMinutes = {},
            onClickAnnouncement = {},
            onStart = {},
            onCancelTimer = {},
        )
    }
}
