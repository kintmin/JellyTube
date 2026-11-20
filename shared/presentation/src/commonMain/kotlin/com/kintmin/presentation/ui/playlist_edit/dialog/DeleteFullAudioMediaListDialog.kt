package com.kintmin.presentation.ui.playlist_edit.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

/**
 * 음원 파일 삭제 확인. [audioMediaName] 을 주면 단건으로, 주지 않으면 [selectedMediaCount] 건으로 묻는다.
 * [canRedownload] 가 false 면 다시 받을 곳이 없다고 알린다 — 기기에서 가져온 음원이 그렇다.
 */
@Composable
fun DeleteFullAudioMediaListDialog(
    isShow: Boolean,
    onDismiss: () -> Unit,
    selectedMediaCount: Int,
    deleteAudioMediaList: () -> Unit,
    audioMediaName: String? = null,
    canRedownload: Boolean = true,
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
                text = "음원 삭제",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                modifier = Modifier.padding(bottom = 12.dp),
                text = buildString {
                    if (audioMediaName != null) {
                        // 이름이 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
                        append("'$audioMediaName' 음원을 지웁니다.")
                    } else {
                        append("${selectedMediaCount}개의 음원을 지웁니다.")
                    }
                    append("\n모든 플레이리스트에서 제거됩니다.")
                    if (canRedownload) {
                        append("\n다시 다운로드해야 재생할 수 있습니다.")
                    } else {
                        append("\n기기에서 가져온 음원이라 다시 받을 수 없습니다.")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                OutlinedButton(
                    // 파괴적 버튼과 간격을 벌려 반사적으로 눌리지 않게 한다.
                    modifier = Modifier.padding(end = 24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    onClick = { onDismiss() }) {
                    Text(
                        text = "취소",
                        fontSize = 14.sp,
                    )
                }
                Button(
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
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

@Preview
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

@Preview
@Composable
fun DeleteSingleAudioMediaDialogPreview() {
    JellyTubeTheme {
        DeleteFullAudioMediaListDialog(
            isShow = true,
            onDismiss = {},
            selectedMediaCount = 1,
            deleteAudioMediaList = {},
            audioMediaName = "밤편지",
        )
    }
}

@Preview
@Composable
fun DeleteNotRedownloadableAudioMediaDialogPreview() {
    JellyTubeTheme {
        DeleteFullAudioMediaListDialog(
            isShow = true,
            onDismiss = {},
            selectedMediaCount = 1,
            deleteAudioMediaList = {},
            audioMediaName = "PC 에서 받은 녹음",
            canRedownload = false,
        )
    }
}
