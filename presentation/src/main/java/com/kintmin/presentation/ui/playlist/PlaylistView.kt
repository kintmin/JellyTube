package com.kintmin.presentation.ui.playlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.playlist.list_item.PlaylistItemAddView
import com.kintmin.presentation.ui.playlist.list_item.PlaylistItemView

@Composable
fun PlaylistView(
    modifier: Modifier,
    data: List<PlaylistItemUiState>,
    sendIntent: (PlaylistIntent) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        items(
            count = data.size,
            key = { index -> data[index].id }
        ) { index ->
            PlaylistItemView(
                modifier = Modifier,
                data = data[index],
                sendIntent = sendIntent,
            )
        }
        item {
            PlaylistItemAddView(
                modifier = Modifier,
                sendIntent = sendIntent,
            )
        }
    }
}

@Preview(showBackground = true)
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