package com.kintmin.presentation.ui.main.playlist.list_item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.domain.playlist.PlaylistTextLimit
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.artworkAccentFallback
import com.kintmin.presentation.theme.contentMuted
import com.kintmin.presentation.theme.destructiveContent
import com.kintmin.presentation.ui.common.JellyTubeDropdownMenu
import com.kintmin.presentation.ui.main.playlist.PlaylistIntent
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState
import com.kintmin.presentation.ui.main.playlist.dialog.PlaylistDeleteDialog
import com.kintmin.presentation.ui.main.playlist.dialog.SecretPlaylistDeleteDialog

@Composable
fun PlaylistItemView(
    modifier: Modifier,
    data: PlaylistItemUiState,
    isNowPlaying: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    sendIntent: (PlaylistIntent) -> Unit,
) {
    var isDropDownExpanded by remember { mutableStateOf(false) }
    var isShowDialog by remember { mutableStateOf(false) }

    if (data.type == PlaylistType.SECRET) {
        SecretPlaylistDeleteDialog(
            showDialog = isShowDialog,
            onDismiss = { isShowDialog = false },
            playlistName = data.name,
            audioMediaCount = data.audioMediaCount,
            deletePlaylist = { sendIntent(PlaylistIntent.OnClickDeletePlaylist(data)) },
            deletePlaylistWithTracks = { sendIntent(PlaylistIntent.OnClickDeletePlaylistWithTracks(data)) },
        )
    } else {
        PlaylistDeleteDialog(
            showDialog = isShowDialog,
            onDismiss = { isShowDialog = false },
            playlistName = data.name,
            audioMediaCount = data.audioMediaCount,
            deletePlaylist = { sendIntent(PlaylistIntent.OnClickDeletePlaylist(data)) },
        )
    }

    // 카드 틀을 두르지 않는다. 설정 화면처럼 배경 위에 그대로 올린다.
    // 안쪽 패딩은 남긴다 — 셀을 꽉 채우면 ink 가 가장자리까지 번져 옆 칸과 붙어 보인다.
    Column(
        modifier = modifier
            // clip 이 없으면 ink 가 각진 사각형으로 번진다. Card 가 하던 일을 여기서 한다.
            .clip(MaterialTheme.shapes.medium)
            .clickable { sendIntent(PlaylistIntent.OnClickPlaylistItem(data)) }
            .padding(12.dp),
    ) {
        PlaylistItemArtworkView(
            imageFileFullPath = data.imageFileFullPath,
            isNowPlaying = isNowPlaying,
            isPlaying = isPlaying,
            accentColor = accentColor,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = data.name,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (data.isDownloadTarget) {
                Icon(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(14.dp),
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "기본 다운로드 플레이리스트",
                    tint = contentMuted,
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                text = data.summaryString,
                style = MaterialTheme.typography.bodySmall,
                color = contentMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // 줄 높이를 차지하지 않고 48dp 터치 영역은 그 밖으로 넘친다. 높이를 잡으면 요약 줄이 부풀어 제목과 벌어진다.
            Box(modifier = Modifier.width(24.dp).height(0.dp)) {
                Box(modifier = Modifier.requiredSize(48.dp)) {
                    IconButton(onClick = { isDropDownExpanded = true }) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "더보기",
                            tint = contentMuted,
                        )
                    }

                    JellyTubeDropdownMenu(
                        expanded = isDropDownExpanded,
                        onDismissRequest = { isDropDownExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("편집하기") },
                            onClick = {
                                isDropDownExpanded = false
                                sendIntent(PlaylistIntent.OnClickModifyPlaylist(data))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("곡 추가하기") },
                            onClick = {
                                isDropDownExpanded = false
                                sendIntent(PlaylistIntent.OnClickAddPlaylist(data))
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (data.isDownloadTarget) "기본 다운로드 플레이리스트" else "기본 다운로드로 지정")
                            },
                            enabled = !data.isDownloadTarget,
                            onClick = {
                                isDropDownExpanded = false
                                sendIntent(PlaylistIntent.OnClickSetDownloadTarget(data))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("삭제하기") },
                            colors = MenuDefaults.itemColors(textColor = destructiveContent),
                            onClick = {
                                isDropDownExpanded = false
                                isShowDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PlaylistItemPreview() {
    JellyTubeTheme {
        PlaylistItemView(
            modifier = Modifier.width(180.dp),
            PlaylistItemUiState.getMock().copy(isDownloadTarget = true),
            isNowPlaying = true,
            isPlaying = true,
            accentColor = artworkAccentFallback,
            sendIntent = {},
        )
    }
}

/** 상한까지 쓴 한글 제목은 두 줄에서 말줄임된다. 전부 보이는 자리는 상세 화면이다. 짧은 제목 칸과는 높이가 다르다. */
@Preview
@Composable
fun PlaylistItemLongNamePreview() {
    JellyTubeTheme {
        Row {
            PlaylistItemView(
                modifier = Modifier.width(180.dp),
                PlaylistItemUiState.getMock().copy(name = "가".repeat(PlaylistTextLimit.MAX_TITLE_LENGTH)),
                isNowPlaying = false,
                isPlaying = false,
                accentColor = artworkAccentFallback,
                sendIntent = {},
            )
            PlaylistItemView(
                modifier = Modifier.width(180.dp),
                PlaylistItemUiState.getMock().copy(name = "드라이브"),
                isNowPlaying = false,
                isPlaying = false,
                accentColor = artworkAccentFallback,
                sendIntent = {},
            )
        }
    }
}