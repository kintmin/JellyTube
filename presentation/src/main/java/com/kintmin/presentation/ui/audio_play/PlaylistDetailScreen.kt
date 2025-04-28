package com.kintmin.presentation.ui.audio_play

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.platform.service.PlaybackService
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.audio_play.list_item.AudioPlayUiState
import com.kintmin.presentation.ui.playlist.PlaylistItemUiState

@Composable
fun PlaylistDetailScreen(
    navigateToBack: () -> Unit,
) {
    val context = LocalContext.current
    val audioPlayViewModel = hiltViewModel<AudioPlayViewModel>()

    val playlistData by audioPlayViewModel.playlistFlow.collectAsState()
    val audioList by audioPlayViewModel.audioListFlow.collectAsState()

    LaunchedEffect(Unit) {
        audioPlayViewModel.eventFlow.collect { event ->
            when (event) {
                AudioPlayEvent.NavigateToBack -> navigateToBack()
                is AudioPlayEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is AudioPlayEvent.RegisterPlaylist -> {
                    context.startService(
                        Intent(context, PlaybackService::class.java).apply {
                            putParcelableArrayListExtra(PlaybackService.EXTRA_PLAYLIST, event.playlist)
                            putExtra(PlaybackService.EXTRA_PLAYLIST_INDEX, event.startIndex)
                            putExtra(PlaybackService.EXTRA_CLEAR_FLAG, event.clearFlag)
                        }
                    )
                }
            }
        }
    }

    PlaylistDetailScreen(
        playlistData = playlistData,
        audioPlayDataList = audioList,
        isBasePlaylist = true,
        sendIntent = audioPlayViewModel::sendIntent,
    )
}

@Composable
fun PlaylistDetailScreen(
    playlistData: PlaylistItemUiState,
    audioPlayDataList: List<AudioPlayUiState>,
    isBasePlaylist: Boolean,
    sendIntent: (AudioPlayIntent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
//        topBar = {
//            TopAppBar(
//                title = { Text(playlistInfo.name) },
//                colors = TopAppBarDefaults.topAppBarColors(),
//            )
//        },
    ) { innerPadding ->
        AudioPlayView(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            playlistData = playlistData,
            audioPlayList = audioPlayDataList,
            isBasePlaylist = isBasePlaylist,
            sendIntent = sendIntent,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistDetailScreenPreview() {
    JellyTubeTheme {
        PlaylistDetailScreen(
            playlistData = PlaylistItemUiState.getMock(),
            audioPlayDataList = AudioPlayUiState.getMockList(),
            isBasePlaylist = true,
            sendIntent = {},
        )
    }
}