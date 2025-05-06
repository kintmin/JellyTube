package com.kintmin.presentation.ui.playlist_edit.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kintmin.presentation.theme.JellyTubeTheme

@Composable
fun DeleteFullAudioMediaListDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    selectedMediaCount: Int,
    deleteAudioMediaList: () -> Unit,
) {
    if (!isShow) return

    Dialog(
        onDismissRequest = { onDismiss() },
        content = {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
                ) {
                    Text(
                        modifier = Modifier.padding(bottom = 8.dp),
                        text = "${selectedMediaCount}개의 파일을 삭제하시겠습니까?",
                        fontSize = 18.sp,
                    )
                    Text(
                        modifier = Modifier.padding(bottom = 8.dp),
                        text = "음원 파일 삭제 시 모든 플레이리스트에서 제거되며, 다시 다운받아야 재생이 가능합니다.",
                        fontSize = 14.sp,
                    )
                    Row(
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        ElevatedButton(
                            modifier = Modifier.padding(end = 8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            onClick = { onDismiss() }) {
                            Text(
                                text = "취소",
                                fontSize = 14.sp,
                            )
                        }
                        ElevatedButton(
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
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
        },
    )
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