package com.kintmin.presentation.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.contentMuted
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
        title = { DetailTopAppBarTitle(text = title) },
        navigationIcon = { BackNavigationIconButton(onClick = onClickBack) },
        actions = actions,
        colors = colors,
    )
}

/**
 * 여러 개를 고르는 화면의 상단바. 고른 것이 있으면 제목이 개수로, 뒤로가기가 선택 해제로 바뀌고
 * 기기 뒤로가기도 화면을 나가기 전에 선택부터 푼다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    title: String,
    selectedCount: Int,
    onClickBack: () -> Unit,
    onClearSelection: () -> Unit,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val isSelecting = selectedCount > 0
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = isSelecting,
    ) {
        onClearSelection()
    }
    TopAppBar(
        expandedHeight = topAppBarHeight,
        title = {
            if (isSelecting) {
                DetailTopAppBarTitle(text = "${selectedCount}곡 선택됨")
            } else {
                Column {
                    DetailTopAppBarTitle(text = title)
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (isSelecting) {
                IconButton(onClick = onClearSelection) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        modifier = Modifier.size(topAppBarIconSize),
                        contentDescription = "선택 해제",
                    )
                }
            } else {
                BackNavigationIconButton(onClick = onClickBack)
            }
        },
        actions = actions,
    )
}

@Composable
private fun DetailTopAppBarTitle(text: String) {
    Text(
        text = text,
        fontSize = topAppBarDetailTitleFontSize,
        lineHeight = topAppBarDetailTitleLineHeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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

@Preview
@Composable
private fun SelectionTopAppBarPreview() {
    JellyTubeTheme {
        Column {
            SelectionTopAppBar(
                title = "곡 추가",
                subtitle = "운동할 때 듣는 음악",
                selectedCount = 0,
                onClickBack = {},
                onClearSelection = {},
            )
            SelectionTopAppBar(
                title = "곡 추가",
                selectedCount = 3,
                onClickBack = {},
                onClearSelection = {},
            )
        }
    }
}

@Preview
@Composable
private fun DetailTopAppBarTitlePreview() {
    JellyTubeTheme {
        DetailTopAppBarTitle(text = "설정")
    }
}
