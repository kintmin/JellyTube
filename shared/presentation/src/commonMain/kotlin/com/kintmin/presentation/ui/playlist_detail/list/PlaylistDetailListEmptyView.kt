package com.kintmin.presentation.ui.playlist_detail.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray40
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.gray80

private val EMPTY_HEIGHT = 240.dp

/**
 * 담긴 음원이 없는 것과 검색에 걸린 음원이 없는 것을 가른다 — 둘을 같은 빈 화면으로 두면
 * 검색이 먹지 않은 것인지 결과가 없는 것인지 구분되지 않는다.
 */
@Composable
fun PlaylistDetailListEmptyView(
    modifier: Modifier,
    searchText: String,
    isBasePlaylist: Boolean,
    onClickAdd: () -> Unit,
) {
    val isSearching = searchText.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = EMPTY_HEIGHT)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = if (isSearching) Icons.Rounded.SearchOff else Icons.Rounded.LibraryMusic,
            contentDescription = null,
            tint = gray40,
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            // 검색어가 사용자 입력이라 조사를 붙이면 받침에서 비문이 된다.
            text = if (isSearching) "'$searchText' 와 일치하는 음원이 없습니다" else "담긴 음원이 없습니다",
            style = MaterialTheme.typography.bodyLarge,
            color = gray80,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = if (isSearching) {
                "제목의 일부만 적어도 찾습니다"
            } else {
                "곡을 담아 두면 한 번에 재생할 수 있습니다"
            },
            style = MaterialTheme.typography.bodySmall,
            color = gray60,
            textAlign = TextAlign.Center,
        )
        if (!isSearching && !isBasePlaylist) {
            Button(
                modifier = Modifier.padding(top = 24.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                onClick = onClickAdd,
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = "곡 추가하기",
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                )
            }
        }
    }
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
