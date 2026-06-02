package com.kintmin.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    searchText: String = "",
    onSearchTextChanged: (String) -> Unit = {}
) {
    var internalText by remember { mutableStateOf(searchText) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(4))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = 8.dp, end = 4.dp),
            imageVector = Icons.Rounded.Search,
            contentDescription = "Search"
        )
        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
                .padding(vertical = 16.dp),
            value = internalText,
            onValueChange = {
                internalText = it
                onSearchTextChanged(it)
            },
            maxLines = 1,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchBarPreview() {
    JellyTubeTheme {
        SearchBar(
            modifier = Modifier,
            searchText = "뭘 검색할까요?",
            onSearchTextChanged = {},
        )
    }
}