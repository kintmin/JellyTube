package com.kintmin.presentation.ui.main.playlist.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.ui.common.JellyTubeDialog

private const val TITLE_MAX_LENGTH = 20

@Composable
fun PlaylistAddDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    existingNames: List<String>,
    makePlaylist: (String) -> Unit,
) {
    JellyTubeDialog(
        showDialog = showDialog,
        onDismiss = onDismiss,
        // 제목을 적는 중에 스크림을 잘못 눌러 입력이 날아가지 않게 한다. 취소로만 닫는다.
        dismissOnClickOutside = false,
    ) {
        var newPlaylistTitle by remember { mutableStateOf("") }
        // 열자마자 빈 칸을 에러로 칠하지 않는다. 손을 댄 뒤부터 말해 준다.
        var hasEdited by remember { mutableStateOf(false) }

        val trimmedTitle = newPlaylistTitle.trim()
        val errorMessage = when {
            trimmedTitle.isBlank() -> "제목을 입력해 주세요"
            trimmedTitle in existingNames -> "같은 이름의 플레이리스트가 이미 있습니다"
            else -> null
        }
        val visibleErrorMessage = errorMessage.takeIf { hasEdited }

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "새 플레이리스트",
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(),
                value = newPlaylistTitle,
                onValueChange = { newText ->
                    hasEdited = true
                    newPlaylistTitle = newText.take(TITLE_MAX_LENGTH)
                },
                isError = visibleErrorMessage != null,
                maxLines = 1,
                label = {
                    Text(
                        text = "제목 (${newPlaylistTitle.length}/$TITLE_MAX_LENGTH)",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                    )
                },
                placeholder = {
                    Text(
                        text = "새 플레이리스트 제목",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        color = contentMuted,
                    )
                },
                supportingText = {
                    // 자리를 늘 차지하게 둬서 메시지가 뜰 때 버튼이 밀리지 않게 한다.
                    Text(
                        text = visibleErrorMessage.orEmpty(),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                    )
                },
            )
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.padding(end = 8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    onClick = { onDismiss() },
                ) {
                    Text(
                        text = "취소",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                    )
                }
                Button(
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 0.dp),
                    onClick = {
                        makePlaylist(trimmedTitle)
                        onDismiss()
                    },
                    enabled = errorMessage == null,
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

@Preview
@Composable
fun PlaylistAddDialogPreview() {
    JellyTubeTheme {
        PlaylistAddDialog(
            showDialog = true,
            onDismiss = {},
            existingNames = listOf("운동할 때"),
            makePlaylist = {},
        )
    }
}
