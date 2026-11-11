package com.kintmin.presentation.ui.setting.data_migration_receive.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageItem
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageKind
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageStatus
import com.kintmin.presentation.ui.setting.data_migration_receive.SettingDataMigrationReceiveUiState

@Composable
fun DataMigrationStageListView(stages: List<DataMigrationStageItem>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            stages.forEach { StageRow(it) }
        }
    }
}

@Composable
private fun StageRow(stage: DataMigrationStageItem) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StageIcon(stage.status)
            Text(
                text = stage.kind.label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            stage.countText()?.let {
                Text(text = it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (stage.status == DataMigrationStageStatus.IN_PROGRESS && stage.totalCount > 0) {
            LinearProgressIndicator(
                progress = { stage.progressFraction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp),
            )
        }
    }
}

@Composable
private fun StageIcon(status: DataMigrationStageStatus) {
    val modifier = Modifier.size(24.dp)
    when (status) {
        DataMigrationStageStatus.PENDING ->
            Icon(Icons.Rounded.RadioButtonUnchecked, null, modifier, tint = MaterialTheme.colorScheme.outline)
        DataMigrationStageStatus.IN_PROGRESS -> CircularProgressIndicator(modifier = modifier.padding(2.dp), strokeWidth = 2.dp)
        DataMigrationStageStatus.DONE ->
            Icon(Icons.Rounded.CheckCircle, null, modifier, tint = MaterialTheme.colorScheme.primary)
        DataMigrationStageStatus.FAILED ->
            Icon(Icons.Rounded.Error, null, modifier, tint = MaterialTheme.colorScheme.error)
    }
}

/** 파일 받기는 바이트로, 나머지는 개수로 진행을 잰다. */
private fun DataMigrationStageItem.progressFraction(): Float = when {
    totalBytes > 0 -> (receivedBytes.toDouble() / totalBytes).toFloat()
    totalCount > 0 -> doneCount.toFloat() / totalCount
    else -> 0f
}

private fun DataMigrationStageItem.countText(): String? {
    if (totalCount == 0) return null
    val count = "$doneCount / $totalCount"
    return if (totalBytes > 0) "$count · ${receivedBytes / MEGABYTE} / ${totalBytes / MEGABYTE} MB" else count
}

private const val MEGABYTE = 1_000_000L

@Preview
@Composable
private fun DataMigrationStageListViewPreview() {
    JellyTubeTheme {
        DataMigrationStageListView(stages = SettingDataMigrationReceiveUiState.getMock().stages)
    }
}

@Preview
@Composable
private fun DataMigrationStageListViewFailedPreview() {
    JellyTubeTheme {
        DataMigrationStageListView(
            stages = listOf(
                DataMigrationStageItem(DataMigrationStageKind.CLEANUP, DataMigrationStageStatus.DONE),
                DataMigrationStageItem(DataMigrationStageKind.PLANNING, DataMigrationStageStatus.DONE),
                DataMigrationStageItem(DataMigrationStageKind.REDOWNLOAD, DataMigrationStageStatus.DONE),
                DataMigrationStageItem(DataMigrationStageKind.TRANSFER, DataMigrationStageStatus.FAILED, 40, 999),
                DataMigrationStageItem(DataMigrationStageKind.APPLY, DataMigrationStageStatus.PENDING),
            ),
        )
    }
}
