package com.kintmin.presentation.ui.main.playlist.dialog

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
fun PlaylistDeleteDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    playlistName: String,
    deletePlaylist: () -> Unit,
) {
    JellyTubeDialog(
        showDialog = showDialog,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "플레이리스트 삭제",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                modifier = Modifier.padding(bottom = 12.dp),
                text = "[$playlistName]를 삭제하시겠습니까?",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                OutlinedButton(
                    modifier = Modifier.padding(end = 8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    onClick = { onDismiss() },
                ) {
                    Text(
                        text = "취소",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                    )
                }
                Button(
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    onClick = {
                        deletePlaylist()
                        onDismiss()
                    },
                ) {
                    Text(
                        text = "삭제하기",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistDeleteDialogPreview() {
    JellyTubeTheme {
        PlaylistDeleteDialog(
            showDialog = true,
            onDismiss = {},
            playlistName = "플레이리스트 예시",
            deletePlaylist = {},
        )
    }
}
