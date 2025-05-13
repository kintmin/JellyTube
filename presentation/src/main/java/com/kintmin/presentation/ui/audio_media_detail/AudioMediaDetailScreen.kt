package com.kintmin.presentation.ui.audio_media_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import java.io.File

@Composable
fun AudioMediaDetailScreen(
    navigateToBack: () -> Unit,
    navigationToAudioMediaEditScreen: (audioMediaId: Int) -> Unit,
    navigateToMainSearchTab: (url: String) -> Unit,
    navigateToPlaylistDetailScreen: (playlistId: Int) -> Unit,
) {
    val mainViewModel = hiltViewModel<AudioMediaDetailViewModel>()

    val data by mainViewModel.data.collectAsState()

    AudioMediaDetailScreen(
        navigateToBack = navigateToBack,
        navigationToAudioMediaEditScreen = navigationToAudioMediaEditScreen,
        navigateToMainSearchTab = navigateToMainSearchTab,
        navigateToPlaylistDetailScreen = navigateToPlaylistDetailScreen,
        data = data,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMediaDetailScreen(
    navigateToBack: () -> Unit,
    navigationToAudioMediaEditScreen: (audioMediaId: Int) -> Unit,
    navigateToMainSearchTab: (url: String) -> Unit,
    navigateToPlaylistDetailScreen: (playlistId: Int) -> Unit,
    data: AudioMediaDetailUiState,
) {
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(data.imageFileFullPath?.let { File(it) }
            ?: androidx.media3.session.R.drawable.media3_icon_artist
        )
        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
        .build()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "미디어 세부정보",
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
        Box(modifier = Modifier.padding(innerPadding)) {
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
        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding() + 210.dp)
                .clip(RoundedCornerShape(16.dp))
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                item(key = -1) {
                    Column(modifier = Modifier.padding(top = 20.dp)) {
                        Text(
                            modifier = Modifier.padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                            text = data.audioMediaName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp,
                        )
                        Text(
                            modifier = Modifier.padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
                            text = "아티스트: ${data.artist}",
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                        )
                        Text(
                            modifier = Modifier.padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
                            text = "재생 시간: ${data.playTime}",
                            fontSize = 16.sp,
                        )
                        Text(
                            modifier = Modifier.padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
                            text = "생성 시각: ${data.audioMediaCreationTime}",
                            fontSize = 16.sp,
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navigateToMainSearchTab(data.source)
                                }
                                .padding(horizontal = 16.dp),
                            text = "출처: ${data.source}",
                            fontSize = 16.sp,
                        )

                        Spacer(
                            modifier = Modifier
                                .padding(vertical = 20.dp, horizontal = 16.dp)
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray)
                        )

                        if (data.audioMediaDescription.isNotBlank()) {
                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = data.audioMediaDescription,
                                fontSize = 16.sp,
                            )

                            Spacer(
                                modifier = Modifier
                                    .padding(vertical = 20.dp, horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray)
                            )
                        }
                        Text(
                            modifier = Modifier.padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
                            text = "추가된 플레이리스트 목록",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                items(
                    count = data.playlists.count(),
                    key = { index -> data.playlists[index].playlistId }
                ) { index ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navigateToPlaylistDetailScreen(data.playlists[index].playlistId)
                            }.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = data.playlists[index].playlistName,
                            fontSize = 20.sp,
                        )
                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = "플레이리스트가 생성된 시각: ${data.playlists[index].playlistCreationTime}",
                            fontSize = 15.sp,
                        )
                        Text(
                            modifier = Modifier.padding(top = 4.dp),
                            text = "플레이리스트에 추가된 시각: ${data.playlists[index].playlistAddedTime}",
                            fontSize = 15.sp,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.wrapContentHeight(),
                verticalAlignment = Alignment.Bottom
            ) {
                TextButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    onClick = {}) {
                    Text(
                        text = "삭제",
                        fontSize = 16.sp,
                    )
                }
                TextButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    onClick = { navigationToAudioMediaEditScreen(data.audioMediaId) }) {
                    Text(
                        text = "수정",
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun AudioMediaDetailScreenPreview() {
    JellyTubeTheme {
        AudioMediaDetailScreen(
            navigateToBack = {},
            navigationToAudioMediaEditScreen = {},
            navigateToMainSearchTab = {},
            navigateToPlaylistDetailScreen = {},
            data = AudioMediaDetailUiState.getMock(),
        )
    }
}