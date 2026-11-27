package com.kintmin.presentation.ui.playlist_edit.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.domain.extension.flattenNewlines
import com.kintmin.domain.playlist.PlaylistTextLimit
import com.kintmin.presentation.ui.common.EditableArtwork
import com.kintmin.presentation.ui.common.UnderlineTextField
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListIntent

@Composable
fun PlaylistEditHeaderView(
    data: PlaylistEditHeaderUiState,
    onClickPickImage: () -> Unit,
    sendIntent: (PlaylistEditListIntent) -> Unit,
) {
    var titleText by remember { mutableStateOf(data.name) }
    // 제목은 DB 에서 첫 프레임 뒤에 도착한다. 그 전의 빈 값을 오류로 그리지 않게, 직접 고친 뒤에만 알린다.
    var hasEditedTitle by remember { mutableStateOf(false) }
    val isTitleError = hasEditedTitle && titleText.isEmpty()
    var descriptionText by remember { mutableStateOf(data.description) }

    LaunchedEffect(data.name.isEmpty()) {
        titleText = data.name
        descriptionText = data.description
    }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            EditableArtwork(
                imageFileFullPath = data.imageFileFullPath,
                isCustomImage = data.isCustomImage,
                onClickPickImage = onClickPickImage,
                onClickResetImage = { sendIntent(PlaylistEditListIntent.OnClickResetPlaylistImage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface),
            )
            Spacer(modifier = Modifier.height(16.dp))
            UnderlineTextField(
                label = "제목",
                value = titleText,
                onValueChange = { input ->
                    val newText = input.flattenNewlines()
                    titleText = newText
                    hasEditedTitle = true
                    if (newText.isNotEmpty()) {
                        sendIntent(PlaylistEditListIntent.OnEditPlaylistTitle(newText))
                    }
                },
                textStyle = MaterialTheme.typography.titleMedium,
                placeholder = "제목 입력",
                maxLength = PlaylistTextLimit.MAX_TITLE_LENGTH,
                singleLine = false,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                errorMessage = if (isTitleError) "제목을 입력해 주세요. 비워 두면 이전 제목이 남습니다." else null,
            )
            Spacer(modifier = Modifier.height(8.dp))
            UnderlineTextField(
                label = "설명",
                value = descriptionText,
                onValueChange = { newText ->
                    descriptionText = newText
                    sendIntent(PlaylistEditListIntent.OnEditPlaylistDescription(newText))
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = "설명 추가",
                maxLength = PlaylistTextLimit.MAX_DESCRIPTION_LENGTH,
                singleLine = false,
            )
        }
    }
}

@Preview
@Composable
fun PlaylistEditHeaderPreview() {
    JellyTubeTheme {
        PlaylistEditHeaderView(
            data = PlaylistEditHeaderUiState.getMock(),
            onClickPickImage = {},
            sendIntent = {},
        )
    }
}