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
fun RingtoneDefaultChangeDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    onConfirmShare: () -> Unit,
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
                text = "벨소리로 등록",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "'계속'을 누르면 공유 화면이 열립니다. 아래쪽 '더 보기'를 눌러 " +
                    "'벨소리로 사용'을 선택해주세요.\n\n" +
                    "등록하면 기기의 기본 벨소리가 이 음원으로 바뀝니다. " +
                    "알람에만 쓰려면 등록 직후 나오는 화면에서 원래 벨소리를 다시 선택해주세요. " +
                    "등록한 음원은 시계 앱의 알람 사운드 목록 '벨소리'에 남아 있습니다.",
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
                        onConfirmShare()
                        onDismiss()
                    },
                ) {
                    Text(
                        text = "계속",
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RingtoneDefaultChangeDialogPreview() {
    JellyTubeTheme {
        RingtoneDefaultChangeDialog(
            isShow = true,
            onDismiss = {},
            onConfirmShare = {},
        )
    }
}
