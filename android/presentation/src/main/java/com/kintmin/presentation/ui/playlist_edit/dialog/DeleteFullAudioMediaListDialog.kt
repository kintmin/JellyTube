package com.kintmin.presentation.ui.playlist_edit.dialog

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
fun DeleteFullAudioMediaListDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    selectedMediaCount: Int,
    deleteAudioMediaList: () -> Unit,
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
                text = "${selectedMediaCount}개의 파일을 삭제하시겠습니까?",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = "음원 파일 삭제 시 모든 플레이리스트에서 제거되며, 다시 다운받아야 재생이 가능합니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                OutlinedButton(
                    modifier = Modifier.padding(end = 8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    onClick = { onDismiss() }) {
                    Text(
                        text = "취소",
                        fontSize = 14.sp,
                    )
                }
                Button(
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 0.dp),
                    onClick = {
                        deleteAudioMediaList()
                        onDismiss()
                    }) {
                    Text(
                        text = "삭제하기",
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeleteFullAudioMediaListDialogPreview() {
    JellyTubeTheme {
        DeleteFullAudioMediaListDialog(
            isShow = true,
            onDismiss = {},
            selectedMediaCount = 4,
            deleteAudioMediaList = {},
        )
    }
}
