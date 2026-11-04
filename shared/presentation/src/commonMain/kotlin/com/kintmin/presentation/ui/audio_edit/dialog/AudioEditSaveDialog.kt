package com.kintmin.presentation.ui.audio_edit.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.bloodyRed100
import com.kintmin.presentation.ui.common.JellyTubeDialog

/** 편집본 저장 방식. */
private enum class SaveMode { Overwrite, NewMedia }

/** 새 음원으로 저장할 때 이름 길이 상한. */
private const val MAX_NAME_LENGTH = 100

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

    LaunchedEffect(isShow) {
        if (isShow) {
            saveMode = SaveMode.Overwrite
            newMediaName = defaultNewMediaName
        }
    }

    JellyTubeDialog(
        showDialog = isShow,
        onDismiss = onDismiss,
        modifier = Modifier.imePadding(),
    ) {
        Column(
            modifier = Modifier
                // 키보드가 올라와 남은 높이보다 내용이 길면 스크롤해서 입력칸과 버튼을 볼 수 있게 한다
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp, horizontal = 24.dp),
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

            Column(modifier = Modifier.selectableGroup()) {
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
            }

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
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            modifier = Modifier.padding(12.dp),
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
