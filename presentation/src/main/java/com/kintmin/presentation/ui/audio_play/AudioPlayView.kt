package com.kintmin.presentation.ui.audio_play

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.audio_play.header.AudioPlayHeaderView
import com.kintmin.presentation.ui.audio_play.list_item.AudioItemView
import com.kintmin.presentation.ui.audio_play.list_item.AudioPlayUiState
import com.kintmin.presentation.ui.playlist.PlaylistItemUiState

@Composable
fun AudioPlayView(
    modifier: Modifier,
    playlistData: PlaylistItemUiState,
    audioPlayList: List<AudioPlayUiState>,
    isBasePlaylist: Boolean,
    sendIntent: (AudioPlayIntent) -> Unit,
) {
    LazyColumn(modifier = modifier) {
        item {
            AudioPlayHeaderView(
                playlistData = playlistData,
                sendIntent = sendIntent,
            )
        }
        items(
            count = audioPlayList.size,
            key = { index -> audioPlayList[index].id }
        ) { index ->
            AudioItemView(
                data = audioPlayList[index],
                isBasePlaylist = isBasePlaylist,
                sendIntent = sendIntent,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MusicControlsPreview() {
    JellyTubeTheme {
        AudioPlayView(
            modifier = Modifier.fillMaxWidth(),
            playlistData = PlaylistItemUiState.getMock(),
            audioPlayList = AudioPlayUiState.getMockList(),
            isBasePlaylist = true,
            sendIntent = {},
        )
    }
}