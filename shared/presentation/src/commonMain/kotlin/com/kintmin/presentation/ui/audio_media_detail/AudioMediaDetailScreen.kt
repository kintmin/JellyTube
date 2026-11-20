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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.softRed
import com.kintmin.presentation.theme.topAppBarDetailTitleFontSize
import com.kintmin.presentation.theme.topAppBarDetailTitleLineHeight
import com.kintmin.presentation.theme.topAppBarHeight
import com.kintmin.presentation.theme.topAppBarIconSize
import com.kintmin.presentation.ui.audio_media_detail.dialog.AudioEditRevertDialog
import com.kintmin.presentation.ui.audio_media_detail.dialog.KaraokeUnlinkDialog
import com.kintmin.presentation.ui.audio_media_detail.view.AudioMediaDetailInfoView
import com.kintmin.presentation.ui.common.FullScreenImageViewer
import com.kintmin.presentation.ui.playlist_edit.dialog.DeleteFullAudioMediaListDialog
import okio.Path.Companion.toPath

@Composable
fun AudioMediaDetailScreen(
    navigateToBack: () -> Unit,
    navigationToAudioMediaInformationEditScreen: (audioMediaId: Int) -> Unit,
    navigateToMainSearchTab: (url: String) -> Unit,
    navigateToPlaylistDetailScreen: (playlistId: Int, audioMediaId: Int) -> Unit,
    navigateToLyricsSearch: (audioMediaId: Int, query: String, durationSeconds: Double?) -> Unit,
    navigateToLyricsViewer: (audioMediaId: Int) -> Unit,
    navigateToKaraokeSearch: (audioMediaId: Int, query: String) -> Unit,
    navigateToAudioEdit: (audioMediaId: Int) -> Unit,
) {
    val mainViewModel = koinViewModel<AudioMediaDetailViewModel>()

    val data by mainViewModel.data.collectAsState()
    val toaster = rememberToaster()

    LaunchedEffect(Unit) {
        mainViewModel.eventFlow.collect { event ->
            when (event) {
                AudioMediaDetailEvent.OnNavigateToBack -> navigateToBack()
                is AudioMediaDetailEvent.ShowToast ->
                    toaster.show(event.message)
            }
        }
    }

    AudioMediaDetailScreen(
        navigateToBack = navigateToBack,
        navigationToAudioMediaInformationEditScreen = navigationToAudioMediaInformationEditScreen,
        navigateToMainSearchTab = navigateToMainSearchTab,
        navigateToPlaylistDetailScreen = navigateToPlaylistDetailScreen,
        navigateToLyricsSearch = navigateToLyricsSearch,
        navigateToLyricsViewer = navigateToLyricsViewer,
        navigateToKaraokeSearch = navigateToKaraokeSearch,
        navigateToAudioEdit = navigateToAudioEdit,
        data = data,
        sendIntent = mainViewModel::sendIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMediaDetailScreen(
    navigateToBack: () -> Unit,
    navigationToAudioMediaInformationEditScreen: (audioMediaId: Int) -> Unit,
    navigateToMainSearchTab: (url: String) -> Unit,
    navigateToPlaylistDetailScreen: (playlistId: Int, audioMediaId: Int) -> Unit,
    navigateToLyricsSearch: (audioMediaId: Int, query: String, durationSeconds: Double?) -> Unit,
    navigateToLyricsViewer: (audioMediaId: Int) -> Unit,
    navigateToKaraokeSearch: (audioMediaId: Int, query: String) -> Unit,
    navigateToAudioEdit: (audioMediaId: Int) -> Unit,
    data: AudioMediaDetailUiState,
    sendIntent: (AudioMediaDetailIntent) -> Unit,
) {
    var isShowDialog by remember { mutableStateOf(false) }
    var isShowKaraokeUnlinkDialog by remember { mutableStateOf(false) }
    var isShowAudioEditRevertDialog by remember { mutableStateOf(false) }
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
        audioMediaName = data.audioMediaName,
        canRedownload = data.webViewUrl != null,
    )

    KaraokeUnlinkDialog(
        isShow = isShowKaraokeUnlinkDialog,
        onDismiss = { isShowKaraokeUnlinkDialog = false },
        onConfirmUnlink = { sendIntent(AudioMediaDetailIntent.OnClickUnlinkKaraokeNumber) },
    )

    AudioEditRevertDialog(
        isShow = isShowAudioEditRevertDialog,
        onDismiss = { isShowAudioEditRevertDialog = false },
        onConfirmRevert = { sendIntent(AudioMediaDetailIntent.OnClickRevertAudioEdit) },
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                expandedHeight = topAppBarHeight,
                title = {
                    Text(
                        text = "미디어 세부정보",
                        fontSize = topAppBarDetailTitleFontSize,
                        lineHeight = topAppBarDetailTitleLineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigateToBack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            modifier = Modifier.size(topAppBarIconSize),
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
                    // 파괴적 동작이라 색과 형태를 수정과 다르게 둔다. primary 글자는 이 배경에서
                    // 본문 대비 바닥값에 못 미치고(3.02:1), 삭제에 긍정 색이 붙는 문제도 있었다.
                    TextButton(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        shape = RectangleShape,
                        enabled = !data.isDeleted && !data.isDeleting,
                        colors = ButtonDefaults.textButtonColors(contentColor = softRed),
                        onClick = { isShowDialog = true }) {
                        if (data.isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = if (data.isDeleting) "삭제 중" else "삭제",
                            fontSize = 14.sp,
                        )
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        shape = RectangleShape,
                        enabled = !data.isDeleted && !data.isDeleting,
                        onClick = { navigationToAudioMediaInformationEditScreen(data.audioMediaId) }) {
                        Text(
                            text = "수정",
                            fontSize = 14.sp,
                        )
                    }
                }
                Box(
                    modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(MaterialTheme.colorScheme.background),
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
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(data.imageFileFullPath.toPath())
                        .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil3.request.CachePolicy.DISABLED)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
        // 리스트가 사진 위를 덮어 터치를 가져가므로 사진 탭은 투명한 첫 칸이 받는다.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
        ) {
            item(key = -2) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clickable(
                            enabled = data.imageFileFullPath != null,
                            interactionSource = null,
                            indication = null,
                        ) { isShowFullScreenImageViewer = true }
                )
            }
            item(key = -1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = 20.dp)
                ) {
                    AudioMediaDetailInfoView(
                        data = data,
                        onClickSource = { url -> navigateToMainSearchTab(url) },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

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
                                    navigateToLyricsSearch(data.audioMediaId, data.audioMediaName, data.audioDurationSeconds)
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

                    if (!data.hasEditedAudio) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    navigateToAudioEdit(data.audioMediaId)
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = if (data.isAudioEditSaving) "편집본 저장 중..." else "음원 편집하기",
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
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { navigateToAudioEdit(data.audioMediaId) },
                                text = if (data.isAudioEditSaving) {
                                    "편집본 저장 중..."
                                } else {
                                    "편집본 사용 중 · ${data.playTime}"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            // 저장이 끝나면 편집본이 다시 연결되므로 저장 중에는 되돌릴 수 없다
                            IconButton(
                                enabled = !data.isAudioEditSaving,
                                onClick = { isShowAudioEditRevertDialog = true },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "원본으로 되돌리기",
                                )
                            }
                        }
                    }

                    Text(
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 16.dp, end = 16.dp),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .clickable {
                            navigateToPlaylistDetailScreen(data.playlists[index].playlistId, data.audioMediaId)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = data.playlists[index].playlistName,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        modifier = Modifier.padding(start = 12.dp),
                        text = "${data.playlists[index].playlistAddedTime} 추가",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun AudioMediaDetailScreenPreview() {
    JellyTubeTheme {
        AudioMediaDetailScreen(
            navigateToBack = {},
            navigationToAudioMediaInformationEditScreen = {},
            navigateToMainSearchTab = {},
            navigateToPlaylistDetailScreen = { _, _ -> },
            navigateToLyricsSearch = { _, _, _ -> },
            navigateToLyricsViewer = {},
            navigateToKaraokeSearch = { _, _ -> },
            navigateToAudioEdit = {},
            data = AudioMediaDetailUiState.getMock(),
            sendIntent = {}
        )
    }
}

@Preview
@Composable
fun AudioMediaDetailScreenDeletingPreview() {
    JellyTubeTheme {
        AudioMediaDetailScreen(
            navigateToBack = {},
            navigationToAudioMediaInformationEditScreen = {},
            navigateToMainSearchTab = {},
            navigateToPlaylistDetailScreen = { _, _ -> },
            navigateToLyricsSearch = { _, _, _ -> },
            navigateToLyricsViewer = {},
            navigateToKaraokeSearch = { _, _ -> },
            navigateToAudioEdit = {},
            data = AudioMediaDetailUiState.getMock().copy(isDeleting = true),
            sendIntent = {}
        )
    }
}

@Preview
@Composable
fun AudioMediaDetailScreenDeletedPreview() {
    JellyTubeTheme {
        AudioMediaDetailScreen(
            navigateToBack = {},
            navigationToAudioMediaInformationEditScreen = {},
            navigateToMainSearchTab = {},
            navigateToPlaylistDetailScreen = { _, _ -> },
            navigateToLyricsSearch = { _, _, _ -> },
            navigateToLyricsViewer = {},
            navigateToKaraokeSearch = { _, _ -> },
            navigateToAudioEdit = {},
            data = AudioMediaDetailUiState.getDeletedMock(),
            sendIntent = {}
        )
    }
}
