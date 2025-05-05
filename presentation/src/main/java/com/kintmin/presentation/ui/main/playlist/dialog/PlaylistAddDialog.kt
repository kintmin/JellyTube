package com.kintmin.presentation.ui.main.playlist.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kintmin.presentation.theme.JellyTubeTheme

@Composable
fun PlaylistAddDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    makePlaylist: (String) -> Unit,
) {
    if (!showDialog) return

    var newPlaylistTitle by remember { mutableStateOf("새로운 플레이리스트") }

    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = "새 플레이리스트",
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textStyle = TextStyle(),
                    value = newPlaylistTitle,
                    onValueChange = { newText ->
                        if (newText.length > 20) return@OutlinedTextField
                        newPlaylistTitle = newText
                    },
                    isError = newPlaylistTitle.isEmpty(),
                    maxLines = 1,
                    label = {
                        Text(
                            text = "제목 (${newPlaylistTitle.length}/20)",
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                        )
                    }
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
                    ElevatedButton(
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        onClick = {
                            makePlaylist(newPlaylistTitle)
                            onDismiss()
                        },
                        enabled = newPlaylistTitle.isNotBlank(),
                    ) {
                        Text(
                            text = "만들기",
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistAddDialogPreview() {
    JellyTubeTheme {
        PlaylistAddDialog(
            showDialog = true,
            onDismiss = {},
            makePlaylist = {},
        )
    }
}