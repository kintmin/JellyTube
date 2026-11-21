package com.kintmin.presentation.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.topAppBarDetailTitleFontSize
import com.kintmin.presentation.theme.topAppBarDetailTitleLineHeight
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.theme.topAppBarIconSize

/** 뒤로가기로 들어온 화면의 상단바. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopAppBar(
    title: String,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
    TopAppBar(
        modifier = modifier,
        expandedHeight = topAppBarHeight,
        title = {
            Text(
                text = title,
                fontSize = topAppBarDetailTitleFontSize,
                lineHeight = topAppBarDetailTitleLineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = { BackNavigationIconButton(onClick = onClickBack) },
        actions = actions,
        colors = colors,
    )
}

@Composable
fun BackNavigationIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Rounded.ArrowBackIosNew,
            modifier = Modifier.size(topAppBarIconSize),
            contentDescription = "뒤로가기",
        )
    }
}

@Preview
@Composable
private fun DetailTopAppBarPreview() {
    JellyTubeTheme {
        DetailTopAppBar(
            title = "설정",
            onClickBack = {},
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        modifier = Modifier.size(topAppBarIconSize),
                        contentDescription = "더보기",
                    )
                }
            },
        )
    }
}
