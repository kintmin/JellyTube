package com.kintmin.presentation.ui.playlist_detail.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.EmptyStateView

private val EMPTY_HEIGHT = 240.dp

/** 둘을 같은 문구로 두면 검색이 먹지 않은 것과 결과가 없는 것이 구분되지 않는다. */
@Composable
fun PlaylistDetailListEmptyView(
    modifier: Modifier,
    searchText: String,
    isBasePlaylist: Boolean,
    onClickAdd: () -> Unit,
) {
    val isSearching = searchText.isNotEmpty()

    EmptyStateView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = EMPTY_HEIGHT),
        icon = if (isSearching) Icons.Rounded.SearchOff else Icons.Rounded.LibraryMusic,
        // 검색어가 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
        title = if (isSearching) "'$searchText' 와 일치하는 음원이 없습니다" else "담긴 음원이 없습니다",
        description = if (isSearching) "제목의 일부만 적어도 찾습니다" else "곡을 담아 두면 한 번에 재생할 수 있습니다",
        actionText = if (!isSearching && !isBasePlaylist) "곡 추가하기" else null,
        onClickAction = onClickAdd,
    )
}

@Preview
@Composable
fun PlaylistDetailListEmptyPreview() {
    JellyTubeTheme {
        PlaylistDetailListEmptyView(
            modifier = Modifier,
            searchText = "",
            isBasePlaylist = false,
            onClickAdd = {},
        )
    }
}

@Preview
@Composable
fun PlaylistDetailListEmptySearchPreview() {
    JellyTubeTheme {
        PlaylistDetailListEmptyView(
            modifier = Modifier,
            searchText = "찾는 제목",
            isBasePlaylist = false,
            onClickAdd = {},
        )
    }
}
