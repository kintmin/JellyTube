package com.kintmin.presentation.ui.main.playlist.dialog

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.domain.extension.flattenNewlines
import com.kintmin.domain.playlist.PlaylistTextLimit
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.DialogButtonRow
import com.kintmin.presentation.ui.common.DialogColumn
import com.kintmin.presentation.ui.common.JellyTubeDialog
import com.kintmin.presentation.ui.common.UnderlineTextField

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
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        val trimmedTitle = newPlaylistTitle.trim()
        val errorMessage = when {
            trimmedTitle.isBlank() -> "제목을 입력해 주세요"
            trimmedTitle in existingNames -> "같은 이름의 플레이리스트가 이미 있습니다"
            else -> null
        }

        DialogColumn(title = "새 플레이리스트") {
            UnderlineTextField(
                modifier = Modifier.focusRequester(focusRequester),
                label = "제목",
                value = newPlaylistTitle,
                onValueChange = { newText ->
                    hasEdited = true
                    newPlaylistTitle = newText.flattenNewlines()
                },
                textStyle = MaterialTheme.typography.titleMedium,
                placeholder = "제목 입력",
                singleLine = false,
                maxLength = PlaylistTextLimit.MAX_TITLE_LENGTH,
                errorMessage = errorMessage.takeIf { hasEdited },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        hasEdited = true
                        if (errorMessage == null) {
                            makePlaylist(trimmedTitle)
                            onDismiss()
                        }
                    },
                ),
            )
            DialogButtonRow(
                dismissText = "취소",
                onDismiss = onDismiss,
                confirmText = "만들기",
                confirmEnabled = errorMessage == null,
                onConfirm = { makePlaylist(trimmedTitle) },
            )
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
