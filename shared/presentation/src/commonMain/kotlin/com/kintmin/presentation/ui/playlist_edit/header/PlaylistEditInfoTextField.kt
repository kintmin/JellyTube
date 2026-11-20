package com.kintmin.presentation.ui.playlist_edit.header

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray20
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.softRed

/**
 * 테두리가 없으니 고칠 수 있다는 신호로 밑줄을 남긴다.
 * 값이 채워지면 placeholder 가 사라지므로 어느 칸인지는 위의 [label] 이 늘 알린다.
 */
@Composable
fun PlaylistEditInfoTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    placeholder: String,
    maxLength: Int,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val contentColor = MaterialTheme.colorScheme.onSurface
    val indicatorColor = when {
        errorMessage != null -> softRed
        isFocused -> contentColor
        else -> gray20
    }

    BasicTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = { newText -> if (newText.length <= maxLength) onValueChange(newText) },
        textStyle = textStyle.copy(color = contentColor),
        singleLine = singleLine,
        cursorBrush = SolidColor(contentColor),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isFocused) contentColor else gray60,
                )
                Box(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = textStyle, color = gray60)
                    }
                    innerTextField()
                }
                HorizontalDivider(
                    thickness = if (isFocused || errorMessage != null) 2.dp else 1.dp,
                    color = indicatorColor,
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = softRed,
                    )
                    Text(
                        text = "${value.length}/$maxLength",
                        style = MaterialTheme.typography.labelSmall,
                        color = gray60,
                    )
                }
            }
        },
    )
}

@Preview
@Composable
fun PlaylistEditInfoTextFieldPreview() {
    JellyTubeTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            PlaylistEditInfoTextField(
                label = "제목",
                value = "드라이브",
                onValueChange = {},
                textStyle = MaterialTheme.typography.titleMedium,
                placeholder = "제목 입력",
                maxLength = 20,
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))
            PlaylistEditInfoTextField(
                label = "설명",
                value = "",
                onValueChange = {},
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = "설명 추가",
                maxLength = 100,
                singleLine = false,
            )
        }
    }
}
