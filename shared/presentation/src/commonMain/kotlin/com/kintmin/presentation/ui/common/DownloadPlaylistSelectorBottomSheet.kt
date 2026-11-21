package com.kintmin.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.main.playlist.list_item.PlaylistAddButtonView

@Composable
fun DownloadPlaylistSelectorBottomSheet(
    playlistList: List<DownloadPlaylistUiState>,
    onSelectPlaylist: (playlistId: Int) -> Unit,
    onDismissRequest: () -> Unit,
    onClickCreatePlaylist: (() -> Unit)? = null,
) {
    JellyTubeBottomSheet(onDismissRequest = onDismissRequest) {
        // 머리말을 Column 으로 감싸면 LazyColumn 이 높이 제약을 잃는다. 같은 리스트에 넣는다.
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "받을 재생목록",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    onClickCreatePlaylist?.let { onClick ->
                        PlaylistAddButtonView(text = "새로 만들기", onClick = onClick)
                    }
                }
            }
            items(
                items = playlistList,
                key = { item -> item.id },
            ) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        // 짙은 남색 배경은 회색 시트 위에서 탁하게 가라앉는다
                        .background(
                            if (item.isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent,
                        )
                        .selectable(
                            selected = item.isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelectPlaylist(item.id) },
                        )
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = item.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.isSelected) {
                        Icon(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DownloadPlaylistSelectorBottomSheetPreview() {
    JellyTubeTheme {
        DownloadPlaylistSelectorBottomSheet(
            playlistList = listOf(
                DownloadPlaylistUiState(
                    id = 1,
                    name = "미분류",
                    isSelected = true,
                ),
                DownloadPlaylistUiState(
                    id = 2,
                    name = "운동할 때 듣는 음악",
                    isSelected = false,
                ),
                DownloadPlaylistUiState(
                    id = 3,
                    name = "출퇴근 플레이리스트",
                    isSelected = false,
                ),
            ),
            onSelectPlaylist = {},
            onDismissRequest = {},
            onClickCreatePlaylist = {},
        )
    }
}
