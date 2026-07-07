package com.kintmin.presentation.ui.audio_media_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.audio_media_detail.dialog.KaraokeUnlinkDialog
import com.kintmin.presentation.ui.common.FullScreenImageViewer
import com.kintmin.presentation.ui.playlist_edit.dialog.DeleteFullAudioMediaListDialog
import java.io.File

@Composable
fun AudioMediaDetailScreen(
    navigateToBack: () -> Unit,
    navigationToAudioMediaEditScreen: (audioMediaId: Int) -> Unit,
    navigateToMainSearchTab: (url: String) -> Unit,
    navigateToPlaylistDetailScreen: (playlistId: Int, audioMediaId: Int) -> Unit,
    navigateToLyricsSearch: (audioMediaId: Int, query: String) -> Unit,
    navigateToLyricsViewer: (audioMediaId: Int) -> Unit,
    navigateToKaraokeSearch: (audioMediaId: Int, query: String) -> Unit,
) {
    val mainViewModel = koinViewModel<AudioMediaDetailViewModel>()

    val data by mainViewModel.data.collectAsState()

    LaunchedEffect(Unit) {
        mainViewModel.eventFlow.collect { event ->
            when (event) {
                AudioMediaDetailEvent.OnNavigateToBack -> navigateToBack()
            }
        }
    }

    AudioMediaDetailScreen(
        navigateToBack = navigateToBack,
        navigationToAudioMediaEditScreen = navigationToAudioMediaEditScreen,
        navigateToMainSearchTab = navigateToMainSearchTab,
        navigateToPlaylistDetailScreen = navigateToPlaylistDetailScreen,
        navigateToLyricsSearch = navigateToLyricsSearch,
        navigateToLyricsViewer = navigateToLyricsViewer,
        navigateToKaraokeSearch = navigateToKaraokeSearch,
        data = data,
        sendIntent = mainViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMediaDetailScreen(
    navigateToBack: () -> Unit,
    navigationToAudioMediaEditScreen: (audioMediaId: Int) -> Unit,
    navigateToMainSearchTab: (url: String) -> Unit,
    navigateToPlaylistDetailScreen: (playlistId: Int, audioMediaId: Int) -> Unit,
    navigateToLyricsSearch: (audioMediaId: Int, query: String) -> Unit,
    navigateToLyricsViewer: (audioMediaId: Int) -> Unit,
    navigateToKaraokeSearch: (audioMediaId: Int, query: String) -> Unit,
    data: AudioMediaDetailUiState,
    sendIntent: (AudioMediaDetailIntent) -> Unit,
) {
    var isShowDialog by remember { mutableStateOf(false) }
    var isShowKaraokeUnlinkDialog by remember { mutableStateOf(false) }
    var isShowFullScreenImageViewer by remember { mutableStateOf(false) }

    FullScreenImageViewer(
        imageFileFullPath = if (isShowFullScreenImageViewer) data.imageFileFullPath else null,
        onDismiss = { isShowFullScreenImageViewer = false },
    )

    DeleteFullAudioMediaListDialog(
        isShow = isShowDialog,
        onDismiss = { isShowDialog = false },
        selectedMediaCount = 1,
        deleteAudioMediaList = { sendIntent(AudioMediaDetailIntent.OnClickDeleteAudioMedia) },
    )

    KaraokeUnlinkDialog(
        isShow = isShowKaraokeUnlinkDialog,
        onDismiss = { isShowKaraokeUnlinkDialog = false },
        onConfirmUnlink = { sendIntent(AudioMediaDetailIntent.OnClickUnlinkKaraokeNumber) },
    )

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
            )
        },
        bottomBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    TextButton(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        shape = RectangleShape,
                        onClick = { isShowDialog = true }) {
                        Text(
                            text = "삭제",
                            fontSize = 16.sp,
                        )
                    }
                    TextButton(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        shape = RectangleShape,
                        onClick = { navigationToAudioMediaEditScreen(data.audioMediaId) }) {
                        Text(
                            text = "수정",
                            fontSize = 16.sp,
                        )
                    }
                }
                Box(
                    modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (data.imageFileFullPath == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(data.imageFileFullPath))
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { isShowFullScreenImageViewer = true }
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(
                    top = innerPadding.calculateTopPadding() + 210.dp,
                    bottom = innerPadding.calculateBottomPadding()
                )
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
                                    if (data.source.startsWith("http://") || data.source.startsWith("https://")) {
                                        navigateToMainSearchTab(data.source)
                                    }
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
                                .background(MaterialTheme.colorScheme.outline)
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
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    if (data.hasLyrics) {
                                        navigateToLyricsViewer(data.audioMediaId)
                                    } else {
                                        navigateToLyricsSearch(data.audioMediaId, data.audioMediaName)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = if (data.hasLyrics) "이 음원의 가사 보기" else "이 음원의 가사 찾기",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                            )
                        }

                        if (data.tjKaraokeNumber == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        navigateToKaraokeSearch(data.audioMediaId, data.audioMediaName)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = "TJ 노래방 번호 찾기",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = "TJ 노래방 번호: ${data.tjKaraokeNumber}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                IconButton(onClick = { isShowKaraokeUnlinkDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "노래방 번호 연동 해제",
                                    )
                                }
                            }
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
                                navigateToPlaylistDetailScreen(data.playlists[index].playlistId, data.audioMediaId)
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
            navigateToPlaylistDetailScreen = { _, _ -> },
            navigateToLyricsSearch = { _, _ -> },
            navigateToLyricsViewer = {},
            navigateToKaraokeSearch = { _, _ -> },
            data = AudioMediaDetailUiState.getMock(),
            sendIntent = {}
        )
    }
}
