package com.kintmin.presentation.ui.playlist_detail.list

import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray80
import com.kintmin.presentation.ui.playlist_edit.dialog.DeleteFullAudioMediaListDialog
import java.io.File

@Composable
fun PlaylistDetailListItemView(
    modifier: Modifier,
    data: PlaylistDetailListItemUiState,
    isBasePlaylist: Boolean,
    sendIntent: (PlaylistDetailListIntent) -> Unit,
) {
    var readModeDropdownExpanded by remember { mutableStateOf(false) }
    var isShowDeleteDialog by remember { mutableStateOf(false) }
    var longPressOffset by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current

    Card(
        modifier,
        shape = RectangleShape,
    ) {
        DeleteFullAudioMediaListDialog(
            isShow = isShowDeleteDialog,
            onDismiss = { isShowDeleteDialog = false },
            selectedMediaCount = 1,
            deleteAudioMediaList = {
                sendIntent(PlaylistDetailListIntent.OnClickDeleteAudioMediaInPlaylist(data))
            },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { itemSize = it }
                .indication(interactionSource = interactionSource, indication = LocalIndication.current)
                .pointerInput(data.id) {
                    detectTapGestures(
                        onPress = { offset ->
                            val press = PressInteraction.Press(offset)
                            interactionSource.emit(press)
                            val released = tryAwaitRelease()
                            val endInteraction = if (released) {
                                PressInteraction.Release(press)
                            } else {
                                PressInteraction.Cancel(press)
                            }
                            interactionSource.emit(endInteraction)
                        },
                        onTap = { sendIntent(PlaylistDetailListIntent.OnClickAudioItem(data)) },
                        onLongPress = { offset ->
                            longPressOffset = offset
                            readModeDropdownExpanded = true
                        },
                    )
                },
        ) {
            Row {
                if (data.imageFileFullPath == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(16))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
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
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(16))
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }

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

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterVertically),
                ) {
                    IconButton(
                        modifier = Modifier.fillMaxHeight(),
                        onClick = { sendIntent(PlaylistDetailListIntent.OnClickShowDetailAudioMedia(data)) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Subject,
                            contentDescription = "세부 화면"
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = readModeDropdownExpanded,
                onDismissRequest = { readModeDropdownExpanded = false },
                offset = DpOffset(
                    x = with(density) { longPressOffset.x.toDp() },
                    y = with(density) {
                        // DropdownMenu y-offset is anchored from the item's bottom.
                        // Subtract item height so the menu appears near the long-press point.
                        (longPressOffset.y - itemSize.height).toDp() + 8.dp
                    },
                ),
            ) {
                DropdownMenuItem(
                    text = { Text("세부 정보") },
                    onClick = {
                        readModeDropdownExpanded = false
                        sendIntent(PlaylistDetailListIntent.OnClickShowDetailAudioMedia(data))
                    },
                )
                DropdownMenuItem(
                    text = { Text("순서 조정") },
                    onClick = {
                        readModeDropdownExpanded = false
                        sendIntent(PlaylistDetailListIntent.OnClickReorderAudioMedia(data))
                    },
                )
                DropdownMenuItem(
                    text = { Text("수정") },
                    onClick = {
                        readModeDropdownExpanded = false
                        sendIntent(PlaylistDetailListIntent.OnClickEditAudioMedia(data))
                    },
                )
                DropdownMenuItem(
                    text = { Text("삭제") },
                    onClick = {
                        readModeDropdownExpanded = false
                        isShowDeleteDialog = true
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistDetailListItemPreview() {
    JellyTubeTheme {
        PlaylistDetailListItemView(
            data = PlaylistDetailListItemUiState.getMock(),
            modifier = Modifier.height(56.dp),
            isBasePlaylist = true,
            sendIntent = {},
        )
    }
}
