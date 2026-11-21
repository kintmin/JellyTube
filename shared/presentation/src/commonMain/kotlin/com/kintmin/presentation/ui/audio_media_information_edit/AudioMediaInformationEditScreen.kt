package com.kintmin.presentation.ui.audio_media_information_edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.domain.audio_media.AudioMediaTextLimit
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.audio_media_detail.view.AudioMediaActionRowView
import com.kintmin.presentation.ui.audio_media_detail.view.AudioMediaActionUnlinkButton
import com.kintmin.presentation.ui.audio_media_information_edit.dialog.RemoveLinkedPlaylistDialog
import com.kintmin.presentation.ui.common.AudioArtwork
import com.kintmin.presentation.ui.common.DetailTopAppBar
import com.kintmin.presentation.ui.common.JellyTubeBottomSheet
import com.kintmin.presentation.ui.common.SectionHeader
import com.kintmin.presentation.ui.common.UnderlineTextField
import com.kintmin.presentation.ui.main.playlist.list_item.PlaylistAddButtonView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AudioMediaInformationEditScreen(
    navigateToBack: () -> Unit,
) {
    val mainViewModel = koinViewModel<AudioMediaInformationEditViewModel>()

    val data by mainViewModel.uiState.collectAsState()

    AudioMediaInformationEditScreen(
        navigateToBack = navigateToBack,
        data = data,
        sendIntent = mainViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMediaInformationEditScreen(
    navigateToBack: () -> Unit,
    data: AudioMediaInformationEditUiState,
    sendIntent: (AudioMediaInformationEditIntent) -> Unit,
) {
    var audioMediaName by remember { mutableStateOf(data.audioMediaName) }
    var artist by remember { mutableStateOf(data.artist) }
    var audioMediaDescription by remember { mutableStateOf(data.audioMediaDescription) }
    // 제목은 DB 에서 첫 프레임 뒤에 도착한다. 그 전의 빈 값을 오류로 그리지 않게, 직접 고친 뒤에만 알린다.
    var hasEditedName by remember { mutableStateOf(false) }
    val isNameError = hasEditedName && audioMediaName.isEmpty()
    var removingPlaylist by remember { mutableStateOf<AudioMediaInformationEditUiState.Playlist?>(null) }

    LaunchedEffect(data.audioMediaName.isEmpty()) {
        audioMediaName = data.audioMediaName
        artist = data.artist
        audioMediaDescription = data.audioMediaDescription
    }

    removingPlaylist?.let { playlist ->
        RemoveLinkedPlaylistDialog(
            showDialog = true,
            onDismiss = { removingPlaylist = null },
            playlistName = playlist.playlistName,
            removeFromPlaylist = {
                sendIntent(AudioMediaInformationEditIntent.OnClickDeleteLinkedPlaylist(playlist.playlistId))
            },
        )
    }

    if (data.isAddPlaylistBottomSheetVisible) {
        AddPlaylistBottomSheet(
            playlists = data.selectablePlaylists,
            onClickPlaylist = { playlistId ->
                sendIntent(AudioMediaInformationEditIntent.OnClickAddLinkedPlaylist(playlistId))
            },
            onDismissRequest = {
                sendIntent(AudioMediaInformationEditIntent.OnDismissAddPlaylistBottomSheet)
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DetailTopAppBar(
                title = "음원 정보 수정",
                onClickBack = { navigateToBack() },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            item {
                AudioArtwork(
                    imageFileFullPath = data.imageFileFullPath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    placeholderIconSize = 36.dp,
                )
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    UnderlineTextField(
                        label = "제목",
                        value = audioMediaName,
                        onValueChange = { newText ->
                            audioMediaName = newText
                            hasEditedName = true
                            if (newText.isNotEmpty()) {
                                sendIntent(AudioMediaInformationEditIntent.OnAudioMediaNameChanged(newText))
                            }
                        },
                        textStyle = MaterialTheme.typography.titleMedium,
                        placeholder = "제목 입력",
                        maxLength = AudioMediaTextLimit.MAX_NAME_LENGTH,
                        singleLine = false,
                        errorMessage = if (isNameError) "제목을 입력해 주세요. 비워 두면 이전 제목이 남습니다." else null,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    UnderlineTextField(
                        label = "아티스트",
                        value = artist,
                        onValueChange = { newText ->
                            artist = newText
                            sendIntent(AudioMediaInformationEditIntent.OnAudioMediaArtistChanged(newText))
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = "아티스트 추가",
                        maxLength = AudioMediaTextLimit.MAX_ARTIST_LENGTH,
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    UnderlineTextField(
                        label = "설명",
                        value = audioMediaDescription,
                        onValueChange = { newText ->
                            audioMediaDescription = newText
                            sendIntent(AudioMediaInformationEditIntent.OnAudioMediaDescriptionChanged(newText))
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = "설명 추가",
                        maxLength = AudioMediaTextLimit.MAX_DESCRIPTION_LENGTH,
                        singleLine = false,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    text = "연결된 플레이리스트",
                    action = {
                        PlaylistAddButtonView(
                            text = "추가",
                            onClick = { sendIntent(AudioMediaInformationEditIntent.OnClickShowAddPlaylistBottomSheet) },
                        )
                    },
                )
            }
            items(
                count = data.playlists.size,
                key = { index -> data.playlists[index].playlistId },
            ) { index ->
                val playlistData = data.playlists[index]
                AudioMediaActionRowView(
                    icon = if (playlistData.isBasePlaylist) Icons.Rounded.Folder else Icons.Rounded.LibraryMusic,
                    title = playlistData.playlistName,
                    onClick = null,
                    trailing = if (playlistData.isBasePlaylist) {
                        null
                    } else {
                        {
                            AudioMediaActionUnlinkButton(
                                contentDescription = "${playlistData.playlistName}에서 빼기",
                                onClick = { removingPlaylist = playlistData },
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AddPlaylistBottomSheet(
    playlists: List<AudioMediaInformationEditUiState.Playlist>,
    onClickPlaylist: (playlistId: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    JellyTubeBottomSheet(onDismissRequest = onDismissRequest) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (playlists.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        text = "추가할 수 있는 플레이리스트가 없습니다.",
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                items(
                    items = playlists,
                    key = { item -> item.playlistId },
                ) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { onClickPlaylist(item.playlistId) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = item.playlistName)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AudioMediaInformationEditScreenPreview() {
    JellyTubeTheme {
        AudioMediaInformationEditScreen(
            navigateToBack = {},
            data = AudioMediaInformationEditUiState.getMock(),
            sendIntent = {},
        )
    }
}
