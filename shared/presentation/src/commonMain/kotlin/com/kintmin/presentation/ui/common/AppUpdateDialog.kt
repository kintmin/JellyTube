package com.kintmin.presentation.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.platform.AppUpdateInstaller
import com.kintmin.presentation.platform.appUpdateInstaller
import com.kintmin.presentation.theme.JellyTubeButtonColors
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.dialogActionsSpacing
import com.kintmin.presentation.theme.dialogButtonSpacing
import com.kintmin.presentation.theme.dialogConfirmButtonPadding
import com.kintmin.presentation.theme.dialogDismissButtonPadding

/**
 * [versionName] 이 null 이면 닫혀 있다. [onIgnore] 를 주면 "이번 버전 무시" 버튼이 생긴다.
 * 업데이트를 누르면 [onDownload] 뒤에 [onDismiss] 까지 부른다.
 */
@Composable
fun AppUpdateDialog(
    versionName: String?,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onIgnore: (() -> Unit)? = null,
    installer: AppUpdateInstaller = appUpdateInstaller,
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
            installer = installer,
        )
    }
}

@Composable
private fun AppUpdateDialogContent(
    versionName: String,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onIgnore: (() -> Unit)?,
    installer: AppUpdateInstaller,
) {
    val guide = when (installer) {
        AppUpdateInstaller.Browser -> "다운로드한 파일을 열어 설치해 주세요."
        AppUpdateInstaller.SideStore -> "SideStore 가 받아서 설치합니다."
    }
    DialogColumn(title = "새 버전이 있습니다") {
        Text(
            text = "JellyTube $versionName 이 나왔습니다.\n$guide",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (onIgnore == null) {
            DialogButtonRow(
                dismissText = "닫기",
                onDismiss = onDismiss,
                confirmText = "업데이트",
                onConfirm = onDownload,
            )
        } else {
            AppUpdateButtonBar(onIgnore = onIgnore, onDismiss = onDismiss, onDownload = onDownload)
        }
    }
}

/** "이번 버전 무시" 는 한 줄에 다 들어가면 시작 쪽에, 아니면 닫기·업데이트 아랫줄 끝에 둔다. */
@Composable
private fun AppUpdateButtonBar(
    onIgnore: () -> Unit,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dialogActionsSpacing),
        content = {
            TextButton(onClick = onIgnore) {
                Text(text = "이번 버전 무시")
            }
            OutlinedButton(
                contentPadding = dialogDismissButtonPadding,
                colors = JellyTubeButtonColors.secondary(),
                onClick = onDismiss,
            ) {
                Text(text = "닫기")
            }
            Button(
                contentPadding = dialogConfirmButtonPadding,
                onClick = {
                    onDownload()
                    onDismiss()
                },
            ) {
                Text(text = "업데이트")
            }
        },
    ) { measurables, constraints ->
        val gap = dialogButtonSpacing.roundToPx()
        val looseConstraints = Constraints(maxWidth = constraints.maxWidth)
        val (ignore, dismiss, download) = measurables.map { it.measure(looseConstraints) }
        val width = constraints.maxWidth
        val actionsWidth = dismiss.width + gap + download.width
        val actionsHeight = maxOf(dismiss.height, download.height)
        val fitsOneRow = ignore.width + gap + actionsWidth <= width
        val height = if (fitsOneRow) maxOf(ignore.height, actionsHeight) else actionsHeight + gap + ignore.height
        layout(width, height) {
            val rowHeight = if (fitsOneRow) height else actionsHeight
            dismiss.placeRelative(width - actionsWidth, (rowHeight - dismiss.height) / 2)
            download.placeRelative(width - download.width, (rowHeight - download.height) / 2)
            if (fitsOneRow) {
                ignore.placeRelative(0, (height - ignore.height) / 2)
            } else {
                ignore.placeRelative(width - ignore.width, actionsHeight + gap)
            }
        }
    }
}

@Preview
@Composable
private fun AppUpdateDialogContentPreview() {
    JellyTubeTheme {
        AppUpdateDialogContent(versionName = "2.1.0", onDownload = {}, onDismiss = {}, onIgnore = null, installer = AppUpdateInstaller.Browser)
    }
}

@Preview
@Composable
private fun AppUpdateDialogContentWithIgnorePreview() {
    JellyTubeTheme {
        AppUpdateDialogContent(versionName = "2.1.0", onDownload = {}, onDismiss = {}, onIgnore = {}, installer = AppUpdateInstaller.Browser)
    }
}

@Preview
@Composable
private fun AppUpdateDialogContentSideStorePreview() {
    JellyTubeTheme {
        AppUpdateDialogContent(versionName = "2.1.0", onDownload = {}, onDismiss = {}, onIgnore = {}, installer = AppUpdateInstaller.SideStore)
    }
}

@Preview
@Composable
private fun AppUpdateDialogContentWithIgnoreNarrowPreview() {
    JellyTubeTheme {
        Box(modifier = Modifier.width(312.dp)) {
            AppUpdateDialogContent(versionName = "2.1.0", onDownload = {}, onDismiss = {}, onIgnore = {}, installer = AppUpdateInstaller.Browser)
        }
    }
}
