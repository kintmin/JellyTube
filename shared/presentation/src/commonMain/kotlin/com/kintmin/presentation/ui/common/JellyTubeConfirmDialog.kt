package com.kintmin.presentation.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeButtonColors
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.dialogActionsSpacing
import com.kintmin.presentation.theme.dialogButtonSpacing
import com.kintmin.presentation.theme.dialogConfirmButtonPadding
import com.kintmin.presentation.theme.dialogContentPadding
import com.kintmin.presentation.theme.dialogDestructiveButtonSpacing
import com.kintmin.presentation.theme.dialogDismissButtonPadding
import com.kintmin.presentation.theme.dialogTitleSpacing

enum class ConfirmStyle {
    Primary,

    /** 되돌릴 수 없다 — 지우거나 편집을 버린다. */
    Destructive,

    /** 되돌릴 수 있는 빼기·해제. */
    Removal,
}

@Composable
fun JellyTubeConfirmDialog(
    showDialog: Boolean,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "취소",
    confirmStyle: ConfirmStyle = ConfirmStyle.Primary,
) {
    JellyTubeDialog(
        showDialog = showDialog,
        onDismiss = onDismiss,
    ) {
        DialogColumn(title = title) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
            DialogButtonRow(
                dismissText = dismissText,
                onDismiss = onDismiss,
                confirmText = confirmText,
                onConfirm = onConfirm,
                confirmStyle = confirmStyle,
            )
        }
    }
}

/** 본문을 직접 그리는 다이얼로그용. [DialogButtonRow] 와 함께 쓴다. */
@Composable
fun DialogColumn(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(dialogContentPadding)) {
        Text(
            modifier = Modifier.padding(bottom = dialogTitleSpacing),
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        content()
    }
}

/** 확인 버튼을 누르면 [onConfirm] 뒤에 [onDismiss] 까지 부른다. */
@Composable
fun ColumnScope.DialogButtonRow(
    dismissText: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    confirmStyle: ConfirmStyle = ConfirmStyle.Primary,
    confirmEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .align(Alignment.End)
            .padding(top = dialogActionsSpacing),
    ) {
        OutlinedButton(
            contentPadding = dialogDismissButtonPadding,
            colors = JellyTubeButtonColors.secondary(),
            onClick = onDismiss,
        ) {
            Text(text = dismissText)
        }
        Spacer(
            modifier = Modifier.width(
                if (confirmStyle == ConfirmStyle.Primary) dialogButtonSpacing else dialogDestructiveButtonSpacing,
            ),
        )
        val onClickConfirm = {
            onConfirm()
            onDismiss()
        }
        when (confirmStyle) {
            ConfirmStyle.Primary -> Button(
                contentPadding = dialogConfirmButtonPadding,
                enabled = confirmEnabled,
                onClick = onClickConfirm,
            ) { Text(text = confirmText) }

            ConfirmStyle.Destructive -> Button(
                contentPadding = dialogConfirmButtonPadding,
                colors = JellyTubeButtonColors.destructive(),
                enabled = confirmEnabled,
                onClick = onClickConfirm,
            ) { Text(text = confirmText) }

            ConfirmStyle.Removal -> OutlinedButton(
                contentPadding = dialogConfirmButtonPadding,
                colors = JellyTubeButtonColors.removal(),
                enabled = confirmEnabled,
                onClick = onClickConfirm,
            ) { Text(text = confirmText) }
        }
    }
}

@Preview
@Composable
private fun JellyTubeConfirmDialogPreview() {
    JellyTubeTheme {
        JellyTubeConfirmDialog(
            showDialog = true,
            title = "플레이리스트 삭제",
            message = "'운동할 때' 플레이리스트를 지웁니다.",
            confirmText = "삭제",
            onConfirm = {},
            onDismiss = {},
            confirmStyle = ConfirmStyle.Destructive,
        )
    }
}

@Preview
@Composable
private fun JellyTubeConfirmDialogRemovalPreview() {
    JellyTubeTheme {
        JellyTubeConfirmDialog(
            showDialog = true,
            title = "플레이리스트에서 빼기",
            message = "'밤편지' 음원을 이 플레이리스트에서 뺍니다.",
            confirmText = "빼기",
            onConfirm = {},
            onDismiss = {},
            confirmStyle = ConfirmStyle.Removal,
        )
    }
}

@Preview
@Composable
private fun DialogColumnPreview() {
    JellyTubeTheme {
        DialogColumn(title = "새 플레이리스트") {
            Text(text = "본문을 직접 그리는 다이얼로그", style = MaterialTheme.typography.bodyMedium)
            DialogButtonRow(
                dismissText = "취소",
                onDismiss = {},
                confirmText = "만들기",
                onConfirm = {},
                confirmEnabled = false,
            )
        }
    }
}
