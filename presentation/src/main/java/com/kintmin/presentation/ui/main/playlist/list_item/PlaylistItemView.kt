package com.kintmin.presentation.ui.main.playlist.list_item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.main.MainScreenIntent
import com.kintmin.presentation.ui.main.MainTabItem
import com.kintmin.presentation.ui.main.playlist.PlaylistIntent
import com.kintmin.presentation.ui.main.playlist.PlaylistItemUiState
import com.kintmin.presentation.ui.main.playlist.dialog.PlaylistDeleteDialog
import java.io.File

@Composable
fun PlaylistItemView(
    modifier: Modifier,
    data: PlaylistItemUiState,
    isBasePlaylist: Boolean,
    sendIntent: (PlaylistIntent) -> Unit,
    sendMainIntent: (MainScreenIntent) -> Unit,
) {
    var isDropDownExpanded by remember { mutableStateOf(false) }
    var isShowDialog by remember { mutableStateOf(false) }

    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(
            data.imageFileFullPath?.let { File(it) } ?: androidx.media3.session.R.drawable.media3_icon_artist)
        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
        .build()

    PlaylistDeleteDialog(
        showDialog = isShowDialog,
        onDismiss = { isShowDialog = false },
        playlistName = data.name,
        deletePlaylist = { sendIntent(PlaylistIntent.OnClickDeletePlaylist(data)) },
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4))
            .clickable { sendIntent(PlaylistIntent.OnClickPlaylistItem(data)) }
            .padding(12.dp),
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(1f)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(4))
                .background(Color.Gray)
        )
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            text = data.name,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.padding(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    modifier = Modifier,
                    text = data.durationString,
                    fontSize = 12.sp,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    modifier = Modifier,
                    text = data.audioMediaCountString,
                    fontSize = 12.sp,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(24.dp),
            ) {
                IconButton(onClick = { isDropDownExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "MoreVert"
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
                        text = { Text("추가하기") },
                        onClick = {
                            isDropDownExpanded = false
                            if (isBasePlaylist) {
                                sendMainIntent(MainScreenIntent.ChangeTab(MainTabItem.Search))
                            } else {
                                sendIntent(PlaylistIntent.OnClickAddPlaylist(data))
                            }
                        }
                    )
                    if (!isBasePlaylist) {
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
}

@Preview(showBackground = true)
@Composable
fun PlaylistItemPreview() {
    JellyTubeTheme {
        PlaylistItemView(
            modifier = Modifier.width(180.dp),
            PlaylistItemUiState.getMock(),
            isBasePlaylist = false,
            sendIntent = {},
            sendMainIntent = {},
        )
    }
}