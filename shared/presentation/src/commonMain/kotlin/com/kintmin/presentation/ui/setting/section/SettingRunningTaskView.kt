package com.kintmin.presentation.ui.setting.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.port.model.RunningTask
import com.kintmin.presentation.theme.JellyTubeTheme

/** 데이터 이전 전에 끝나 있어야 하는 작업이 남아 있을 때 보여 준다. */
@Composable
fun SettingRunningTaskView(
    runningTasks: Map<RunningTask, Int>,
    onClickRecheck: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("아직 끝나지 않은 작업이 있습니다", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "아래 작업이 모두 끝난 뒤에 데이터를 옮길 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
            )
            runningTasks.forEach { (task, count) ->
                Text("· ${task.label} ${count}건", style = MaterialTheme.typography.bodyMedium)
            }
            if (RunningTask.PC_FILE_SHARE in runningTasks) {
                Text(
                    text = "PC 연결은 설정 → PC에서 공유받기에서 끊어 주세요.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = onClickRecheck) { Text("다시 확인") }
        }
    }
}

@Preview
@Composable
private fun SettingRunningTaskViewPreview() {
    JellyTubeTheme {
        SettingRunningTaskView(
            runningTasks = mapOf(RunningTask.YOUTUBE_DOWNLOAD to 2, RunningTask.PC_FILE_SHARE to 1),
            onClickRecheck = {},
        )
    }
}
