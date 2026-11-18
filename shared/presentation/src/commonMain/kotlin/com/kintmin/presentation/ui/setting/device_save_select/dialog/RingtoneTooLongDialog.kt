package com.kintmin.presentation.ui.setting.device_save_select.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.JellyTubeDialog

@Composable
fun RingtoneTooLongDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    onConfirmEdit: () -> Unit,
) {
    JellyTubeDialog(
        showDialog = isShow,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
        ) {
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = "30초 미만이어야 합니다",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "벨소리·알람음으로는 30초 미만의 음원만 등록할 수 있습니다. 음원을 편집해서 줄이시겠습니까?",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.align(Alignment.End),
            ) {
                OutlinedButton(
                    modifier = Modifier.padding(end = 8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    onClick = onDismiss,
                ) {
                    Text(
                        text = "취소",
                        fontSize = 14.sp,
                    )
                }
                Button(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    onClick = {
                        onConfirmEdit()
                        onDismiss()
                    },
                ) {
                    Text(
                        text = "편집하기",
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RingtoneTooLongDialogPreview() {
    JellyTubeTheme {
        RingtoneTooLongDialog(
            isShow = true,
            onDismiss = {},
            onConfirmEdit = {},
        )
    }
}
