package com.kintmin.presentation.ui.playlist_detail

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.platform.util.MediaControllerManager
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.playlist_detail.list_item.AudioPlayUiState
import com.kintmin.presentation.ui.playlist.PlaylistItemUiState
import com.kintmin.presentation.ui.playlist_detail.list_item.toMediaItem

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistData: PlaylistItemUiState,
    audioPlayDataList: List<AudioPlayUiState>,
    isBasePlaylist: Boolean,
    sendIntent: (AudioPlayIntent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        sendIntent(AudioPlayIntent.OnClickNavigationBack)
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "ArrowBackIosNew"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
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