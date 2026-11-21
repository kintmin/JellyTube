package com.kintmin.presentation.ui.main.youtube_search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Link
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.PillTextField
import com.kintmin.presentation.ui.common.PillTextFieldTrailingButton
import com.kintmin.presentation.ui.common.SearchField

/** SearchField 와 같은 pill 이다. 지우기 대신 보내기를 두므로 따로 둔다. */
@Composable
fun YoutubeUrlField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    PillTextField(
        modifier = modifier,
        value = value,
        placeholder = "이동할 URL 붙여넣기",
        onValueChange = onValueChange,
        leadingIcon = Icons.Rounded.Link,
        onImeSearch = onSubmit,
        trailingIcon = {
            PillTextFieldTrailingButton(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "URL 열기",
                onClick = onSubmit,
            )
        },
    )
}

@Preview
@Composable
private fun YoutubeUrlFieldPreview() {
    JellyTubeTheme {
        YoutubeUrlField(value = "", onValueChange = {}, onSubmit = {})
    }
}
