package com.kintmin.presentation.ui.audio_media_edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.presentation.theme.JellyTubeTheme
import java.io.File

@Composable
fun AudioMediaEditScreen(
    navigateToBack: () -> Unit,
) {
    val mainViewModel = hiltViewModel<AudioMediaEditViewModel>()

    val data by mainViewModel.data.collectAsState()

    AudioMediaEditScreen(
        navigateToBack = navigateToBack,
        data = data,
        sendIntent = mainViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMediaEditScreen(
    navigateToBack: () -> Unit,
    data: AudioMediaEditUiState,
    sendIntent: (AudioMediaEditIntent) -> Unit,
) {
    var audioMediaName by remember { mutableStateOf(data.audioMediaName) }
    var artist by remember { mutableStateOf(data.artist) }
    var audioMediaDescription by remember { mutableStateOf(data.audioMediaDescription) }

    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(data.imageFileFullPath?.let { File(it) }
            ?: androidx.media3.session.R.drawable.media3_icon_artist
        )
        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
        .build()

    LaunchedEffect(data.audioMediaName.isEmpty()) {
        audioMediaName = data.audioMediaName
        artist = data.artist
        audioMediaDescription = data.audioMediaDescription
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "미디어 수정",
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                    )
                },
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
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            item {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(220.dp)
                        .background(Color.Gray)
                )
            }
            item {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                    textStyle = TextStyle(),
                    value = audioMediaName,
                    onValueChange = { newText ->
                        if (newText.length > 100) return@OutlinedTextField
                        audioMediaName = newText
                        if (audioMediaName.isNotEmpty()) {
                            sendIntent(AudioMediaEditIntent.OnAudioMediaNameChanged(newText))
                        }
                    },
                    isError = audioMediaName.isEmpty(),
                    maxLines = 2,
                    label = {
                        Text(
                            text = "미디어 이름 (${audioMediaName.length}/100)",
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                        )
                    }
                )
            }
            item {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                    textStyle = TextStyle(),
                    value = artist,
                    onValueChange = { newText ->
                        if (newText.length > 100) return@OutlinedTextField
                        artist = newText
                        sendIntent(AudioMediaEditIntent.OnAudioMediaArtistChanged(newText))
                    },
                    maxLines = 2,
                    label = {
                        Text(
                            text = "아티스트 (${artist.length}/50)",
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                        )
                    }
                )
            }
            item {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    textStyle = TextStyle(),
                    value = audioMediaDescription,
                    onValueChange = { newText ->
                        if (newText.length > 100) return@OutlinedTextField
                        audioMediaDescription = newText
                        sendIntent(AudioMediaEditIntent.OnAudioMediaDescriptionChanged(newText))
                    },
                    label = {
                        Text(
                            text = "미디어 설명 (${audioMediaDescription.length}/100)",
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                        )
                    }
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "연결된 플레이리스트",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp,
                )
            }
            items(
                count = data.playlists.size,
                key = { index -> data.playlists[index].playlistId },
            ) { index ->
                val playlistData = data.playlists[index]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        text = playlistData.playlistName,
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                    )

                    if (!Playlist.isBasePlaylist(playlistData.playlistId)) {
                        IconButton(
                            modifier = Modifier,
                            onClick = { sendIntent(AudioMediaEditIntent.OnClickDeleteLinkedPlaylist(playlistData.playlistId)) },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Cancel,
                                contentDescription = "Cancel"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AudioMediaEditScreenPreview() {
    JellyTubeTheme {
        AudioMediaEditScreen(
            navigateToBack = {},
            data = AudioMediaEditUiState.getMock(),
            sendIntent = {},
        )
    }
}