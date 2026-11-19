package com.kintmin.presentation.ui.main.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.ui.main.playlist.list_item.PlaylistAddButtonView
import com.kintmin.presentation.ui.main.playlist.list_item.PlaylistItemView
import com.kintmin.presentation.ui.main.playlist.list_item.PlaylistLibraryBlockView

/**
 * 성격이 다른 둘을 다른 컴포넌트로 그린다. 위는 시스템이 관리하는 라이브러리(1열 리스트 행),
 * 아래는 사용자가 만든 플레이리스트(2열 카드)다. 한 격자 안에서 span 으로 나눈다.
 */
@Composable
fun PlaylistView(
    modifier: Modifier,
    data: List<PlaylistItemUiState>,
    sendIntent: (PlaylistIntent) -> Unit,
) {
    val libraryItems = data.filter { it.isBasePlaylist }
    val userItems = data.filterNot { it.isBasePlaylist }

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(2),
        // 좌우 여백을 격자에 주지 않는다. 라이브러리 행의 ink 가 가장자리까지 닿아야 한다.
        // 가장자리 여백은 각 항목이 자기 안쪽 패딩으로 만든다.
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        if (libraryItems.isNotEmpty()) {
            item(key = "library-header", span = { GridItemSpan(maxLineSpan) }) {
                PlaylistSectionLabel(text = "라이브러리")
            }
            item(key = "library-block", span = { GridItemSpan(maxLineSpan) }) {
                PlaylistLibraryBlockView(
                    modifier = Modifier.fillMaxWidth(),
                    data = libraryItems,
                    sendIntent = sendIntent,
                )
            }
        }

        item(key = "user-header", span = { GridItemSpan(maxLineSpan) }) {
            PlaylistSectionLabel(
                text = "내 플레이리스트",
                showDivider = libraryItems.isNotEmpty(),
                action = {
                    IconButton(
                        // 바꿀 것이 없으면 빈 시트가 올라온다. 누를 수 없게 막는다.
                        enabled = userItems.size > 1,
                        onClick = { sendIntent(PlaylistIntent.OnClickShowReorderBottomSheet) },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapVert,
                            contentDescription = "플레이리스트 순서 변경",
                            tint = gray60,
                        )
                    }
                    PlaylistAddButtonView(
                        existingNames = data.map { it.name },
                        sendIntent = sendIntent,
                    )
                },
            )
        }
        items(
            count = userItems.size,
            key = { index -> userItems[index].id }
        ) { index ->
            PlaylistItemView(
                modifier = Modifier,
                data = userItems[index],
                sendIntent = sendIntent,
            )
        }
    }
}

/** 설정 화면의 섹션 머리말과 같은 모양. 구분선과 여백으로만 구역을 가른다. */
@Composable
private fun PlaylistSectionLabel(
    text: String,
    showDivider: Boolean = false,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        top = if (showDivider) 12.dp else 8.dp,
                        bottom = 8.dp,
                    )
                    .semantics { heading() },
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = gray60,
            )
            action?.invoke(this)
        }
    }
}

@Preview
@Composable
fun PlaylistPreview() {
    JellyTubeTheme {
        PlaylistView(
            modifier = Modifier.fillMaxSize(),
            PlaylistItemUiState.getMockList(),
            sendIntent = {},
        )
    }
}
