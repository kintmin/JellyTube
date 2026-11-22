package com.kintmin.presentation.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.kintmin.presentation.theme.dialogConfirmButtonPadding
import com.kintmin.presentation.theme.dialogDismissButtonPadding

/**
 * [versionName] 이 null 이면 닫혀 있다. [onIgnore] 를 주면 "이번 버전 무시" 버튼이 생긴다.
 * 다운로드를 누르면 [onDownload] 뒤에 [onDismiss] 까지 부른다.
 */
@Composable
fun AppUpdateDialog(
    versionName: String?,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onIgnore: (() -> Unit)? = null,
) {
    JellyTubeDialog(
        showDialog = versionName != null,
        onDismiss = onDismiss,
    ) {
        AppUpdateDialogContent(
            versionName = versionName.orEmpty(),
            onDownload = onDownload,
            onDismiss = onDismiss,
            onIgnore = onIgnore,
        )
    }
}

@Composable
private fun AppUpdateDialogContent(
    versionName: String,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onIgnore: (() -> Unit)?,
) {
    DialogColumn(title = "새 버전이 있습니다") {
        Text(
            text = "JellyTube $versionName 이 나왔습니다.\n다운로드한 파일을 열어 설치해 주세요.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (onIgnore == null) {
            DialogButtonRow(
                dismissText = "닫기",
                onDismiss = onDismiss,
                confirmText = "다운로드",
                onConfirm = onDownload,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dialogActionsSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onIgnore) {
                    Text(text = "이번 버전 무시")
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    contentPadding = dialogDismissButtonPadding,
                    colors = JellyTubeButtonColors.secondary(),
                    onClick = onDismiss,
                ) {
                    Text(text = "닫기")
                }
                Spacer(modifier = Modifier.width(dialogButtonSpacing))
                Button(
                    contentPadding = dialogConfirmButtonPadding,
                    onClick = {
                        onDownload()
                        onDismiss()
                    },
                ) {
                    Text(text = "다운로드")
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppUpdateDialogContentPreview() {
    JellyTubeTheme {
        AppUpdateDialogContent(versionName = "2.1.0", onDownload = {}, onDismiss = {}, onIgnore = null)
    }
}

@Preview
@Composable
private fun AppUpdateDialogContentWithIgnorePreview() {
    JellyTubeTheme {
        AppUpdateDialogContent(versionName = "2.1.0", onDownload = {}, onDismiss = {}, onIgnore = {})
    }
}
