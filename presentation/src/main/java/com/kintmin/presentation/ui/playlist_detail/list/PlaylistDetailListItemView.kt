package com.kintmin.presentation.ui.playlist_detail.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Reorder
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import java.io.File

@Composable
fun PlaylistDetailListItemView(
    modifier: Modifier,
    data: PlaylistDetailListItemUiState,
    isEditMode: Boolean,
    isBasePlaylist: Boolean,
    draggingItemId: Int?,
    onDragStart: (Offset, Int) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    sendIntent: (PlaylistDetailListIntent) -> Unit,
) {
    var readModeDropdownExpanded by remember { mutableStateOf(false) }

    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(
            data.imageFileFullPath?.let { File(it) }
                ?: androidx.media3.session.R.drawable.media3_icon_artist
        )
        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
        .build()

    Row(modifier = modifier
        .fillMaxWidth()
        .zIndex(1f.takeIf { data.id == draggingItemId } ?: 0f)
        .drawBehind {
            if (data.id == draggingItemId) {
                drawLine(
                    color = Color(0xFFDADADA),
                    strokeWidth = 0.5.dp.toPx(),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f)
                )
                drawLine(
                    color = Color(0xFFDADADA),
                    strokeWidth = 0.5.dp.toPx(),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height)
                )
            }
        }
        .clickable {
            sendIntent(PlaylistDetailListIntent.OnClickAudioItem(data))
        }
    ) {
        if (isEditMode) {
            IconButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically),

                onClick = { }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cancel,
                    contentDescription = "Cancel",
                    tint = Color(0xFFEE1111)
                )
            }
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .padding(8.dp)
                .clip(RoundedCornerShape(16))
                .background(Color.Gray)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = data.mediaName,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = data.subTitle,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isEditMode) {
            IconButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .pointerInput(data.id, data.sequence) {
                        detectDragGestures(
                            onDragStart = {
                                onDragStart(it, data.id)
                            },
                            onDrag = { change, dragAmount ->
                                onDrag(change, dragAmount)
                            },
                            onDragEnd = {
                                onDragEnd()
                            },
                        )
                    },
                onClick = {

                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Reorder,
                    contentDescription = "Reorder"
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically),
            ) {
                IconButton(
                    modifier = Modifier
                        .fillMaxHeight(),
                    onClick = { readModeDropdownExpanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = readModeDropdownExpanded,
                    onDismissRequest = { readModeDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("정보 수정") },
                        onClick = {
                            readModeDropdownExpanded = false
                            sendIntent(PlaylistDetailListIntent.OnClickShowDetailAudioMedia(data))
                        }
                    )
                    if (!isBasePlaylist) {
                        DropdownMenuItem(
                            text = { Text("플레이리스트에서 제거") },
                            onClick = {
                                readModeDropdownExpanded = false
                                sendIntent(PlaylistDetailListIntent.OnClickDeleteAudioMediaInPlaylist(data))
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("음원 제거") },
                        onClick = {
                            readModeDropdownExpanded = false
                            sendIntent(PlaylistDetailListIntent.OnClickDeleteAudioMediaFile(data))
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AudioItemEditModePreview() {
    JellyTubeTheme {
        PlaylistDetailListItemView(
            data = PlaylistDetailListItemUiState.getMock(),
            modifier = Modifier.height(56.dp),
            isEditMode = true,
            isBasePlaylist = true,
            draggingItemId = null,
            onDragStart = { _, _ -> },
            onDrag = { _, _ -> },
            onDragEnd = {},
            sendIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AudioItemReadModePreview() {
    JellyTubeTheme {
        PlaylistDetailListItemView(
            data = PlaylistDetailListItemUiState.getMock(),
            modifier = Modifier.height(56.dp),
            isEditMode = false,
            isBasePlaylist = true,
            sendIntent = {},
            draggingItemId = null,
            onDragStart = { _, _ -> },
            onDrag = { _, _ -> },
            onDragEnd = {},
        )
    }
}