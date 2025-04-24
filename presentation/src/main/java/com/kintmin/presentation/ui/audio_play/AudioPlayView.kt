package com.kintmin.presentation.ui.audio_play

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kintmin.presentation.theme.YTMusicBoxTheme
import com.kintmin.presentation.ui.audio_play.header.AudioPlayHeaderView
import com.kintmin.presentation.ui.audio_play.list_item.AudioItemView
import com.kintmin.presentation.ui.audio_play.list_item.AudioPlayUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun AudioPlayView(
    modifier: Modifier,
    audioPlayDataListFlow: Flow<List<AudioPlayUiState>>,
    isBasePlaylist: Boolean,
    sendIntent: (AudioPlayIntent) -> Unit,
) {
    val audioList by audioPlayDataListFlow.collectAsState(initial = emptyList())

    LazyColumn(modifier = modifier) {
        item {
            AudioPlayHeaderView(sendIntent)
        }
        items(
            count = audioList.size,
            key = { index -> audioList[index].id }
        ) { index ->
            AudioItemView(
                data = audioList[index],
                isBasePlaylist = isBasePlaylist,
                sendIntent = sendIntent,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MusicControlsPreview() {
    YTMusicBoxTheme {
        AudioPlayView(
            modifier = Modifier.fillMaxWidth(),
            audioPlayDataListFlow = flowOf(AudioPlayUiState.getMockList()),
            isBasePlaylist = true,
            sendIntent = {},
        )
    }
}