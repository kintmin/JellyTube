package com.kintmin.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    searchText: String = "",
    placeholder: String = "",
    onSearchTextChanged: (String) -> Unit = {}
) {
    var internalText by remember { mutableStateOf(searchText) }
    // 입력과 placeholder 의 줄 높이가 다르면 빈 칸일 때 커서가 placeholder 높이에 맞춰 위로 뜬다.
    val textStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .padding(start = 16.dp, end = 8.dp)
                .size(20.dp),
            tint = gray60,
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
        )
        BasicTextField(
            modifier = Modifier
                .weight(1f)
                .padding(end = if (internalText.isEmpty()) 16.dp else 0.dp),
            value = internalText,
            onValueChange = {
                internalText = it
                onSearchTextChanged(it)
            },
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (internalText.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = gray60,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (internalText.isNotEmpty()) {
            IconButton(
                modifier = Modifier.padding(end = 4.dp),
                onClick = {
                    internalText = ""
                    onSearchTextChanged("")
                },
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "검색어 전체 삭제",
                    tint = gray60,
                )
            }
        }
    }
}

@Preview
@Composable
fun SearchBarPreview() {
    JellyTubeTheme {
        SearchBar(
            modifier = Modifier,
            placeholder = "곡 이름 검색",
            onSearchTextChanged = {},
        )
    }
}