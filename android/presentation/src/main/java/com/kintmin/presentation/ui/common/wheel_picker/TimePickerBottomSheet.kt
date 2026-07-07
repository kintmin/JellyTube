package com.kintmin.presentation.ui.common.wheel_picker

import android.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.kintmin.presentation.theme.JellyTubeTheme

/** audioDurationMs 가 null 일 때 시/분 컬럼의 기본 상한(99분). */
private const val DEFAULT_MAX_MS = 99 * 60_000L

/**
 * 가사 시작 시간을 시/분/초/밀리초 휠로 조정하는 BottomSheet.
 * 음원 길이가 1시간 이상이면 [시][분][초][밀리초], 아니면 [분][초][밀리초] 컬럼을 노출한다.
 * 밀리초 컬럼은 센티초(00~95)를 0.05초(50ms) 단위로만 조정한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        KeepBottomSheetNavigationBarDark()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "시작 시간 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showHours) {
                    NumberWheelPicker(
                        values = hourValues,
                        selectedValue = hours,
                        onValueChange = { hours = it },
                        label = "시",
                        modifier = Modifier.weight(1f),
                    )
                }
                NumberWheelPicker(
                    values = minuteValues,
                    selectedValue = minutes,
                    onValueChange = { minutes = it },
                    label = "분",
                    modifier = Modifier.weight(1f),
                )
                NumberWheelPicker(
                    values = secondValues,
                    selectedValue = seconds,
                    onValueChange = { seconds = it },
                    label = "초",
                    modifier = Modifier.weight(1f),
                )
                NumberWheelPicker(
                    values = centiValues,
                    selectedValue = centiseconds,
                    onValueChange = { centiseconds = it },
                    label = "밀리초",
                    modifier = Modifier.weight(1f),
                )
            }

            Button(
                onClick = {
                    val h = if (showHours) hours else 0
                    val totalMs = ((h * 60L + minutes) * 60L + seconds) * 1_000L + centiseconds * 10L
                    onConfirm(totalMs)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                Text("확인")
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun KeepBottomSheetNavigationBarDark() {
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect {
        dialogWindow?.let { window ->
            window.navigationBarColor = Color.BLACK
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightNavigationBars = false
            }
        }
    }
}

@Preview(showBackground = true)
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
