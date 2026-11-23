package com.kintmin.presentation.ui.main.playlist.dialog

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.DialogChoiceButtonRow
import com.kintmin.presentation.ui.common.DialogColumn
import com.kintmin.presentation.ui.common.JellyTubeDialog

/**
 * 곡을 남기면 미분류로 돌아가 "전체" 에도 나타난다 — 비밀로 담아 둔 것이 한 번에 드러나므로
 * 묻지 않고 지우지 않는다.
 */
@Composable
fun SecretPlaylistDeleteDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    playlistName: String,
    audioMediaCount: Int,
    deletePlaylist: () -> Unit,
    deletePlaylistWithTracks: () -> Unit,
) {
    JellyTubeDialog(
        showDialog = showDialog,
        onDismiss = onDismiss,
    ) {
        DialogColumn(title = "비밀 플레이리스트 삭제") {
            Text(
                // 이름이 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
                text = buildString {
                    append("'$playlistName' 플레이리스트를 지웁니다.")
                    if (audioMediaCount > 0) {
                        append("\n\n담긴 ${audioMediaCount}곡을 남기면 미분류로 가고 '전체' 에도 나타납니다.")
                        append("\n곡까지 지우면 파일이 함께 삭제되며 되돌릴 수 없습니다.")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            DialogChoiceButtonRow(
                onDismiss = onDismiss,
                removalText = if (audioMediaCount > 0) "곡은 남기기" else "삭제",
                onRemoval = deletePlaylist,
                destructiveText = "곡까지 삭제".takeIf { audioMediaCount > 0 },
                onDestructive = deletePlaylistWithTracks,
            )
        }
    }
}

@Preview
@Composable
fun SecretPlaylistDeleteDialogPreview() {
    JellyTubeTheme {
        SecretPlaylistDeleteDialog(
            showDialog = true,
            onDismiss = {},
            playlistName = "나만 듣는 곡",
            audioMediaCount = 5,
            deletePlaylist = {},
            deletePlaylistWithTracks = {},
        )
    }
}
