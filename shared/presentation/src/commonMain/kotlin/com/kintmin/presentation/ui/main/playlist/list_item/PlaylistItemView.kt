package com.kintmin.presentation.ui.main.playlist.list_item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.ui.main.playlist.PlaylistIntent
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState
import com.kintmin.presentation.ui.main.playlist.dialog.PlaylistDeleteDialog
import okio.Path.Companion.toPath

@Composable
fun PlaylistItemView(
    modifier: Modifier,
    data: PlaylistItemUiState,
    sendIntent: (PlaylistIntent) -> Unit,
) {
    var isDropDownExpanded by remember { mutableStateOf(false) }
    var isShowDialog by remember { mutableStateOf(false) }

    PlaylistDeleteDialog(
        showDialog = isShowDialog,
        onDismiss = { isShowDialog = false },
        playlistName = data.name,
        audioMediaCount = data.audioMediaCount,
        deletePlaylist = { sendIntent(PlaylistIntent.OnClickDeletePlaylist(data)) },
    )

    // 카드 틀을 두르지 않는다. 설정 화면처럼 배경 위에 그대로 올린다.
    // 안쪽 패딩은 남긴다 — 셀을 꽉 채우면 ink 가 가장자리까지 번져 옆 칸과 붙어 보인다.
    Column(
        modifier = modifier
            // clip 이 없으면 ink 가 각진 사각형으로 번진다. Card 가 하던 일을 여기서 한다.
            .clip(MaterialTheme.shapes.medium)
            .clickable { sendIntent(PlaylistIntent.OnClickPlaylistItem(data)) }
            .padding(12.dp),
    ) {
        val artworkModifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)

        if (data.imageFileFullPath == null) {
            Box(
                modifier = artworkModifier,
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = gray60,
                )
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(data.imageFileFullPath.toPath())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = artworkModifier,
            )
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = data.summaryString,
                    style = MaterialTheme.typography.bodySmall,
                    color = gray60,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(onClick = { isDropDownExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "더보기",
                        tint = gray60,
                    )
                }

                DropdownMenu(
                    expanded = isDropDownExpanded,
                    onDismissRequest = { isDropDownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("수정하기") },
                        onClick = {
                            isDropDownExpanded = false
                            sendIntent(PlaylistIntent.OnClickModifyPlaylist(data))
                        }
                    )
                    DropdownMenuItem(
                        // "추가" 가 이 화면에서 세 가지를 뜻했다. 여기는 곡을 넣는 쪽이다.
                        text = { Text("곡 추가하기") },
                        onClick = {
                            isDropDownExpanded = false
                            sendIntent(PlaylistIntent.OnClickAddPlaylist(data))
                        }
                    )
                    // 파괴적 동작을 바로 위 항목과 붙여 두지 않는다. 색으로 가르지 않는 것은
                    // 팔레트의 두 빨강이 이 배경에서 본문 대비 바닥값에 못 미쳐서다.
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("삭제하기") },
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

@Preview
@Composable
fun PlaylistItemPreview() {
    JellyTubeTheme {
        PlaylistItemView(
            modifier = Modifier.width(180.dp),
            PlaylistItemUiState.getMock(),
            sendIntent = {},
        )
    }
}