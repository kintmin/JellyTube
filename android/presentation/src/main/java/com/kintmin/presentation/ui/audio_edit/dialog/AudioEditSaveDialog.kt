package com.kintmin.presentation.ui.audio_edit.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.bloodyRed100
import com.kintmin.presentation.ui.common.JellyTubeDialog

/**
 * 편집본 저장 확인.
 *
 * 편집본이 이미 있으면 교체된다는 점과, 싱크 가사가 있으면 타임스탬프가 어긋난다는 점을 알려준다.
 */
@Composable
fun AudioEditSaveDialog(
    isShow: Boolean,
    resultPlayTime: String,
    hasEditedAudio: Boolean,
    hasSyncedLyrics: Boolean,
    onDismiss: () -> Unit,
    onConfirmOverwrite: () -> Unit,
) {
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
                text = "편집 결과 길이는 $resultPlayTime 입니다. 원본 파일은 지우지 않아 언제든 되돌릴 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (hasEditedAudio) {
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = "이미 있는 편집본은 새 편집본으로 교체됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (hasSyncedLyrics) {
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
                    onClick = {
                        onConfirmOverwrite()
                        onDismiss()
                    },
                ) {
                    Text(text = "저장")
                }
            }
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
            hasEditedAudio = true,
            hasSyncedLyrics = true,
            onDismiss = {},
            onConfirmOverwrite = {},
        )
    }
}
