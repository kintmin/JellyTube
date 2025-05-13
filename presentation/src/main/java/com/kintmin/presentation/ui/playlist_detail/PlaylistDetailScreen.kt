package com.kintmin.presentation.ui.playlist_detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderEvent
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderIntent
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderView
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListItemUiState
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderUiState
import com.kintmin.presentation.ui.playlist_detail.header.PlaylistDetailHeaderViewModel
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListEvent
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListIntent
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListItemView
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListViewModel

@Composable
fun PlaylistDetailScreen(
    navigateToBack: () -> Unit,
    navigateToAddAudioMediaScreen: (playlistId: Int) -> Unit,
    navigateToPlaylistEditScreen: (playlistId: Int) -> Unit,
    navigateToAudioDetailScreen: (audioMediaId: Int) -> Unit,
) {
    val headerViewModel = hiltViewModel<PlaylistDetailHeaderViewModel>()
    val listViewModel = hiltViewModel<PlaylistDetailListViewModel>()

    val headerData by headerViewModel.headerDataFlow.collectAsState()
    val audioList by listViewModel.audioListFlow.collectAsState()

    LaunchedEffect(Unit) {
        headerViewModel.eventFlow.collect { event ->
            when (event) {
                PlaylistDetailHeaderEvent.NavigateToAddAudioMediaScreen -> navigateToAddAudioMediaScreen(headerViewModel.playlistId)
                PlaylistDetailHeaderEvent.NavigateToEditPlaylistScreen -> navigateToPlaylistEditScreen(headerViewModel.playlistId)
            }
        }
    }

    LaunchedEffect(Unit) {
        listViewModel.eventFlow.collect { event ->
            when (event) {
                is PlaylistDetailListEvent.NavigateToAudioDetailScreen -> navigateToAudioDetailScreen(event.audioMediaId)
            }
        }
    }

    PlaylistDetailScreen(
        navigateToBack = navigateToBack,
        headerData = headerData,
        audioPlayDataList = audioList,
        isBasePlaylist = headerViewModel.isBasePlaylist,
        sendPlaylistDetailListIntent = listViewModel::sendIntent,
        sendPlaylistDetailHeaderIntent = headerViewModel::sendIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    navigateToBack: () -> Unit,
    headerData: PlaylistDetailHeaderUiState,
    audioPlayDataList: List<PlaylistDetailListItemUiState>,
    isBasePlaylist: Boolean,
    sendPlaylistDetailListIntent: (PlaylistDetailListIntent) -> Unit,
    sendPlaylistDetailHeaderIntent: (PlaylistDetailHeaderIntent) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navigateToBack() }) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                PlaylistDetailHeaderView(
                    headerData = headerData,
                    isBasePlaylist = isBasePlaylist,
                    sendIntent = sendPlaylistDetailHeaderIntent,
                )
            }
            itemsIndexed(
                items = audioPlayDataList,
                key = { _, item -> item.id }
            ) { _, item ->
                Box(modifier = Modifier.animateItem()) {
                    PlaylistDetailListItemView(
                        data = item,
                        modifier = Modifier.height(80.dp),
                        isBasePlaylist = isBasePlaylist,
                        sendIntent = sendPlaylistDetailListIntent,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistDetailScreenPreview() {
    JellyTubeTheme {
        PlaylistDetailScreen(
            navigateToBack = {},
            headerData = PlaylistDetailHeaderUiState.getMock(),
            audioPlayDataList = PlaylistDetailListItemUiState.getMockList(),
            isBasePlaylist = false,
            sendPlaylistDetailListIntent = {},
            sendPlaylistDetailHeaderIntent = {},
        )
    }
}
