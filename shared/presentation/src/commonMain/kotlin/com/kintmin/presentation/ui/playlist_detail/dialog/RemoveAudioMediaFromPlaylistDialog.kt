package com.kintmin.presentation.ui.playlist_detail.dialog

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

/**
 * [mediaName] 을 주면 단건으로, 주지 않으면 [selectedMediaCount] 건으로 묻는다.
 * [isSecretPlaylist] 면 곡이 어디로 가는지까지 알린다 — 비밀에서 빠지면 드러나는 곳으로 간다.
 */
@Composable
fun RemoveAudioMediaFromPlaylistDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    removeFromPlaylist: () -> Unit,
    deleteAudioMedia: () -> Unit,
    mediaName: String? = null,
    selectedMediaCount: Int = 1,
    isSecretPlaylist: Boolean = false,
) {
    JellyTubeDialog(showDialog = showDialog, onDismiss = onDismiss) {
        DialogColumn(title = "플레이리스트에서 빼기") {
            Text(
                text = buildString {
                    if (mediaName != null) {
                        // 이름이 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
                        append("'$mediaName' 음원을 이 플레이리스트에서 뺍니다.")
                    } else {
                        append("${selectedMediaCount}곡을 이 플레이리스트에서 뺍니다.")
                    }
                    append("\n\n곡을 남기면 음원 파일은 그대로라 다시 추가할 수 있습니다.")
                    if (isSecretPlaylist) {
                        append(" 다른 비밀 플레이리스트에 없으면 미분류로 가고 '전체' 에도 나타납니다.")
                    }
                    append("\n곡까지 지우면 파일이 함께 삭제되며 모든 플레이리스트에서 사라집니다.")
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = dialogActionsSpacing),
            ) {
                TextButton(onClick = onDismiss) { Text("취소") }
                Spacer(modifier = Modifier.width(dialogButtonSpacing))
                OutlinedButton(
                    colors = JellyTubeButtonColors.removal(),
                    onClick = {
                        removeFromPlaylist()
                        onDismiss()
                    },
                ) {
                    Text(text = "곡은 남기기")
                }
                Spacer(modifier = Modifier.width(dialogButtonSpacing))
                Button(
                    colors = JellyTubeButtonColors.destructive(),
                    onClick = {
                        deleteAudioMedia()
                        onDismiss()
                    },
                ) {
                    Text(text = "곡까지 삭제")
                }
            }
        }
    }
}

@Preview
@Composable
fun RemoveAudioMediaFromPlaylistDialogPreview() {
    JellyTubeTheme {
        RemoveAudioMediaFromPlaylistDialog(
            showDialog = true,
            onDismiss = {},
            removeFromPlaylist = {},
            deleteAudioMedia = {},
            mediaName = "음원 제목 예시",
        )
    }
}

@Preview
@Composable
fun RemoveAudioMediaFromSecretPlaylistDialogPreview() {
    JellyTubeTheme {
        RemoveAudioMediaFromPlaylistDialog(
            showDialog = true,
            onDismiss = {},
            removeFromPlaylist = {},
            deleteAudioMedia = {},
            selectedMediaCount = 3,
            isSecretPlaylist = true,
        )
    }
}
