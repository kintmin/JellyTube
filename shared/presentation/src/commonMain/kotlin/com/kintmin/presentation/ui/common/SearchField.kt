package com.kintmin.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.theme.floatingElevation
import com.kintmin.presentation.theme.outlineSubtle

/**
 * X 는 입력이 있으면 지우고, 비어 있으면 [onCloseWhenEmpty] 가 있을 때만 닫기로 남는다 — 비어
 * 있을 때 지울 것이 없으니 닫는 것이 유일하게 뜻이 있다.
 */
@Composable
fun SearchField(
    modifier: Modifier = Modifier,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onSearch: (() -> Unit)? = null,
    onCloseWhenEmpty: (() -> Unit)? = null,
) {
    val isEmpty = value.isEmpty()
    PillTextField(
        modifier = modifier,
        value = value,
        placeholder = placeholder,
        onValueChange = onValueChange,
        leadingIcon = Icons.Rounded.Search,
        onImeSearch = onSearch,
        trailingIcon = if (!isEmpty || onCloseWhenEmpty != null) {
            {
                PillTextFieldTrailingButton(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = if (isEmpty) "검색 닫기" else "검색어 전체 삭제",
                    onClick = { if (isEmpty) onCloseWhenEmpty?.invoke() else onValueChange("") },
                )
            }
        } else {
            null
        },
    )
}

/** 앞 아이콘·placeholder·뒤 버튼을 가진 둥근 한 줄 입력칸. */
@Composable
fun PillTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    onImeSearch: (() -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    // 입력과 placeholder 의 줄 높이가 다르면 빈 칸일 때 커서가 placeholder 높이에 맞춰 위로 뜬다.
    val textStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .padding(start = 16.dp, end = 8.dp)
                .size(20.dp),
            tint = contentMuted,
            imageVector = leadingIcon,
            contentDescription = null,
        )
        BasicTextField(
            modifier = Modifier
                .weight(1f)
                .padding(end = if (trailingIcon != null) 0.dp else 16.dp),
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            keyboardOptions = if (onImeSearch != null) KeyboardOptions(imeAction = ImeAction.Search) else KeyboardOptions.Default,
            keyboardActions = KeyboardActions(onSearch = onImeSearch?.let { { it() } }),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = contentMuted,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            },
        )
        trailingIcon?.invoke()
    }
}

@Composable
fun PillTextFieldTrailingButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = Modifier.padding(end = 4.dp),
        onClick = onClick,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = contentMuted,
        )
    }
}

/** 목록·웹뷰 위에 뜬 필드. 다크 테마에서는 그림자가 배경에 묻혀 테두리를 함께 두른다. */
fun Modifier.floatingField(shape: Shape = CircleShape): Modifier = this
    .shadow(elevation = floatingElevation, shape = shape)
    .border(width = 1.dp, color = outlineSubtle, shape = shape)

/** [floatingField] 를 감싼 바깥 여백(위아래 8dp)까지 더한 높이. 아래 목록이 이만큼 비켜 준다. */
val FloatingSearchFieldHeight = 64.dp

@Preview
@Composable
private fun SearchFieldPreview() {
    JellyTubeTheme {
        Column {
            SearchField(value = "", placeholder = "노래 제목으로 검색", onValueChange = {}, onSearch = {})
            SearchField(value = "밤편지", placeholder = "노래 제목으로 검색", onValueChange = {}, onSearch = {})
        }
    }
}

@Preview
@Composable
private fun PillTextFieldPreview() {
    JellyTubeTheme {
        PillTextField(
            value = "",
            placeholder = "이동할 URL 붙여넣기",
            onValueChange = {},
            leadingIcon = Icons.Rounded.Search,
            trailingIcon = {
                PillTextFieldTrailingButton(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "검색 닫기",
                    onClick = {},
                )
            },
        )
    }
}
