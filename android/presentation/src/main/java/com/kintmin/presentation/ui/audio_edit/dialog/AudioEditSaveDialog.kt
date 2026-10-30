package com.kintmin.presentation.ui.audio_edit.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.bloodyRed100
import com.kintmin.presentation.ui.common.JellyTubeDialog

/** 편집본 저장 방식. */
private enum class SaveMode { Overwrite, NewMedia }

/** 새 음원으로 저장할 때 이름 길이 상한. */
private const val MAX_NAME_LENGTH = 100

/**
 * 편집본 저장 방식을 고르는 다이얼로그.
 *
 * 덮어쓰기는 플레이리스트·가사·노래방 번호를 그대로 유지하고 되돌릴 수 있다.
 * 새 음원으로 저장하면 원본과 완전히 분리돼 둘 다 남는다.
 */
@Composable
fun AudioEditSaveDialog(
    isShow: Boolean,
    resultPlayTime: String,
    defaultNewMediaName: String,
    hasEditedAudio: Boolean,
    hasSyncedLyrics: Boolean,
    onDismiss: () -> Unit,
    onConfirmOverwrite: () -> Unit,
    onConfirmSaveAsNew: (name: String) -> Unit,
) {
    var saveMode by remember { mutableStateOf(SaveMode.Overwrite) }
    var newMediaName by remember { mutableStateOf(defaultNewMediaName) }

    // 다이얼로그를 다시 열 때마다 기본 이름으로 되돌린다
    LaunchedEffect(isShow) {
        if (isShow) {
            saveMode = SaveMode.Overwrite
            newMediaName = defaultNewMediaName
        }
    }

    JellyTubeDialog(
        showDialog = isShow,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
        ) {
            Text(
                modifier = Modifier.padding(vertical = 12.dp),
                text = "편집한 음원을 저장할까요?",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "편집 결과 길이는 $resultPlayTime 입니다. 원본 파일은 지우지 않습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )

            SaveModeRow(
                isSelected = saveMode == SaveMode.Overwrite,
                title = "이 음원 덮어쓰기",
                description = "플레이리스트·가사·노래방 번호가 그대로 유지되고 언제든 되돌릴 수 있습니다." +
                    if (hasEditedAudio) " 기존 편집본은 교체됩니다." else "",
                onClick = { saveMode = SaveMode.Overwrite },
            )
            SaveModeRow(
                isSelected = saveMode == SaveMode.NewMedia,
                title = "새 음원으로 저장",
                description = "원본과 별개인 음원이 하나 더 생깁니다. 썸네일과 노래방 번호는 물려받고 가사는 가져오지 않습니다.",
                onClick = { saveMode = SaveMode.NewMedia },
            )

            if (saveMode == SaveMode.NewMedia) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    value = newMediaName,
                    onValueChange = {
                        if (it.length > MAX_NAME_LENGTH) return@OutlinedTextField
                        newMediaName = it
                    },
                    label = { Text(text = "새 음원 이름") },
                    isError = newMediaName.isBlank(),
                    singleLine = true,
                )
            }

            if (hasSyncedLyrics && saveMode == SaveMode.Overwrite) {
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = "이 음원에는 시간이 맞춰진 가사가 있습니다. 길이가 바뀌면 가사 표시 시점이 어긋나므로 가사를 다시 맞춰야 합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = bloodyRed100,
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(text = "취소")
                }
                Button(
                    enabled = saveMode == SaveMode.Overwrite || newMediaName.isNotBlank(),
                    onClick = {
                        when (saveMode) {
                            SaveMode.Overwrite -> onConfirmOverwrite()
                            SaveMode.NewMedia -> onConfirmSaveAsNew(newMediaName)
                        }
                        onDismiss()
                    },
                ) {
                    Text(text = "저장")
                }
            }
        }
    }
}

@Composable
private fun SaveModeRow(
    isSelected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
        )
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview
@Composable
private fun AudioEditSaveDialogPreview() {
    JellyTubeTheme {
        AudioEditSaveDialog(
            isShow = true,
            resultPlayTime = "00:03:45",
            defaultNewMediaName = "음원 제목 (편집본)",
            hasEditedAudio = true,
            hasSyncedLyrics = true,
            onDismiss = {},
            onConfirmOverwrite = {},
            onConfirmSaveAsNew = {},
        )
    }
}
