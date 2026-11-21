package com.kintmin.presentation.ui.common.wheel_picker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.JellyTubeBottomSheet

/** audioDurationMs 가 null 일 때 시/분 컬럼의 기본 상한(99분). */
private const val DEFAULT_MAX_MS = 99 * 60_000L

/**
 * 가사 시작 시간을 시/분/초/밀리초 휠로 조정하는 BottomSheet.
 * 음원 길이가 1시간 이상이면 시:분:초.1/100초, 아니면 분:초.1/100초 휠을 노출한다.
 * 마지막 컬럼은 1/100초(00~95)를 0.05초(50ms) 단위로만 조정한다.
 */
@Composable
fun TimePickerBottomSheet(
    initialTimeMs: Long,
    audioDurationMs: Long?,
    onConfirm: (timeMs: Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val totalDurationMs = (audioDurationMs ?: DEFAULT_MAX_MS).coerceAtLeast(0L)
    val maxHours = (totalDurationMs / 3_600_000L).toInt()
    val showHours = maxHours >= 1
    val maxMinutesNoHour = (totalDurationMs / 60_000L).toInt().coerceAtLeast(0)

    val snapped = (initialTimeMs / 50L) * 50L

    var hours by remember { mutableIntStateOf((snapped / 3_600_000L).toInt()) }
    var minutes by remember {
        mutableIntStateOf(
            if (showHours) ((snapped % 3_600_000L) / 60_000L).toInt() else (snapped / 60_000L).toInt(),
        )
    }
    var seconds by remember { mutableIntStateOf(((snapped % 60_000L) / 1_000L).toInt()) }
    var centiseconds by remember { mutableIntStateOf(((snapped % 1_000L) / 10L).toInt()) }

    val hourValues = (0..maxHours).toList()
    val minuteValues = (0..(if (showHours) 59 else maxMinutesNoHour)).toList()
    val secondValues = (0..59).toList()
    val centiValues = (0..95 step 5).toList()

    JellyTubeBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 목록 시트와 달리 내용이 가운데 대칭이라 제목도 가운데 둔다. 왼쪽에 두면 혼자 비껴 보인다.
            Text(
                modifier = Modifier.padding(bottom = 12.dp),
                text = "시작 시간 설정",
                style = MaterialTheme.typography.titleLarge,
            )

            // 열 이름 대신 구분자를 둬 편집 화면의 시간 표기(00:05.50)와 같은 모양으로 읽히게 한다.
            Box(contentAlignment = Alignment.Center) {
                WheelPickerHighlight()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showHours) {
                        Wheel(hourValues, hours) { hours = it }
                        WheelSeparator(":")
                    }
                    Wheel(minuteValues, minutes) { minutes = it }
                    WheelSeparator(":")
                    Wheel(secondValues, seconds) { seconds = it }
                    WheelSeparator(".")
                    Wheel(centiValues, centiseconds) { centiseconds = it }
                }
            }

            // 목록 위에 뜨는 BottomActionButton 과 달리 시트 바닥에 붙은 버튼이라 그림자를 주지 않는다.
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
                    .height(48.dp),
                onClick = {
                    val h = if (showHours) hours else 0
                    val totalMs = ((h * 60L + minutes) * 60L + seconds) * 1_000L + centiseconds * 10L
                    onConfirm(totalMs)
                },
            ) {
                Text("적용")
            }
        }
    }
}

@Composable
private fun RowScope.Wheel(values: List<Int>, selected: Int, onChange: (Int) -> Unit) {
    NumberWheelPicker(
        values = values,
        selectedValue = selected,
        onValueChange = onChange,
        showHighlight = false,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun WheelSeparator(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Preview
@Composable
private fun WheelPreview() {
    JellyTubeTheme {
        Row {
            Wheel((0..59).toList(), 5) {}
        }
    }
}

@Preview
@Composable
private fun WheelSeparatorPreview() {
    JellyTubeTheme {
        WheelSeparator(":")
    }
}

@Preview
@Composable
private fun TimePickerBottomSheetPreview() {
    JellyTubeTheme {
        TimePickerBottomSheet(
            initialTimeMs = 62_450L,
            audioDurationMs = 4 * 60_000L + 30_000L,
            onConfirm = {},
            onDismissRequest = {},
        )
    }
}
