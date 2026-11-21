package com.kintmin.presentation.ui.main.floating_action.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeButtonColors
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.dialogActionsSpacing
import com.kintmin.presentation.theme.dialogButtonSpacing
import com.kintmin.presentation.ui.common.DialogColumn
import com.kintmin.presentation.ui.common.JellyTubeDialog

/** 재생목록에서 연 영상은 그 곡만 받을지 재생목록 전체를 받을지 묻는다. 고르는 버튼이 둘이라 취소를 따로 둔다. */
@Composable
fun DownloadChoiceDialog(
    showDialog: Boolean,
    onChooseSingle: () -> Unit,
    onChoosePlaylist: () -> Unit,
    onDismiss: () -> Unit,
) {
    JellyTubeDialog(showDialog = showDialog, onDismiss = onDismiss) {
        DialogColumn(title = "재생목록 전체를 받을까요?") {
            Text(
                text = "재생목록에서 연 영상입니다. 이 곡만 받거나, 재생목록의 곡을 모두 받아 같은 이름의 재생목록에 담을 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = dialogActionsSpacing),
            ) {
                TextButton(onClick = onDismiss) { Text("취소") }
                Spacer(modifier = Modifier.width(dialogButtonSpacing))
                OutlinedButton(onClick = onChooseSingle, colors = JellyTubeButtonColors.secondary()) { Text("이 곡만") }
                Spacer(modifier = Modifier.width(dialogButtonSpacing))
                Button(onClick = onChoosePlaylist) { Text("전체 받기") }
            }
        }
    }
}

@Preview
@Composable
private fun DownloadChoiceDialogPreview() {
    JellyTubeTheme {
        DownloadChoiceDialog(showDialog = true, onChooseSingle = {}, onChoosePlaylist = {}, onDismiss = {})
    }
}
