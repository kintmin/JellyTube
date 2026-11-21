package com.kintmin.presentation.ui.common

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.outlineSubtle
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.theme.destructiveContent

/**
 * 테두리가 없으니 고칠 수 있다는 신호로 밑줄을 남긴다.
 * 값이 채워지면 placeholder 가 사라지므로 어느 칸인지는 위의 [label] 이 알린다.
 * 같은 칸이 줄마다 반복돼 문맥이 이미 말해 주면 [label] 을 비운다.
 */
@Composable
fun UnderlineTextField(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    placeholder: String,
    singleLine: Boolean,
    maxLength: Int? = null,
    underlineOnlyWhenFocused: Boolean = false,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val contentColor = MaterialTheme.colorScheme.onSurface
    val indicatorColor = when {
        errorMessage != null -> destructiveContent
        isFocused -> contentColor
        // 자리는 남겨 두고 색만 지운다. 포커스가 들어올 때 줄 높이가 흔들리지 않게.
        underlineOnlyWhenFocused -> Color.Transparent
        else -> outlineSubtle
    }

    BasicTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        // 제한보다 길게 저장된 값도 있어 줄이는 편집은 막지 않는다. 막으면 한 글자도 지울 수 없다.
        onValueChange = { newText ->
            if (maxLength == null || newText.length <= maxLength || newText.length < value.length) onValueChange(newText)
        },
        textStyle = textStyle.copy(color = contentColor),
        singleLine = singleLine,
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(contentColor),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Column {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isFocused) contentColor else contentMuted,
                    )
                }
                Box(modifier = Modifier.padding(top = if (label != null) 4.dp else 0.dp, bottom = 8.dp)) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = textStyle, color = contentMuted)
                    }
                    innerTextField()
                }
                HorizontalDivider(
                    thickness = if (isFocused || errorMessage != null) 2.dp else 1.dp,
                    color = indicatorColor,
                )
                if (maxLength != null || errorMessage != null) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = destructiveContent,
                        )
                        if (maxLength != null) {
                            Text(
                                text = "${value.length}/$maxLength",
                                style = MaterialTheme.typography.labelSmall,
                                color = contentMuted,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Preview
@Composable
fun UnderlineTextFieldPreview() {
    JellyTubeTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            UnderlineTextField(
                label = "제목",
                value = "드라이브",
                onValueChange = {},
                textStyle = MaterialTheme.typography.titleMedium,
                placeholder = "제목 입력",
                maxLength = 20,
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))
            UnderlineTextField(
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
