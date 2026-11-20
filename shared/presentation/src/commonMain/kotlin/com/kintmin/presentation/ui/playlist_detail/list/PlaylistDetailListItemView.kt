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
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.gray80
import com.kintmin.presentation.theme.softRed
import com.kintmin.presentation.ui.playlist_edit.dialog.DeleteFullAudioMediaListDialog
import okio.Path.Companion.toPath

@Composable
fun PlaylistDetailListItemView(
    modifier: Modifier,
    data: PlaylistDetailListItemUiState,
    isBasePlaylist: Boolean,
    sendIntent: (PlaylistDetailListIntent) -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isShowDeleteDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

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
                        // 롱프레스는 호출부의 끌어서 순서 변경이 받는다. 여기서 받으면 그쪽이 굶는다.
                        onTap = { sendIntent(PlaylistDetailListIntent.OnClickAudioItem(data)) },
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
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(data.imageFileFullPath.toPath())
                            .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil3.request.CachePolicy.DISABLED)
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
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                    IconButton(
                        modifier = Modifier.size(48.dp),
                        onClick = { isMenuExpanded = true },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "더보기",
                            modifier = Modifier.size(20.dp),
                            tint = gray60,
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        // M3 기본 메뉴 모서리는 extraSmall(4dp) 이라 각져 보인다.
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        DropdownMenuItem(
                            text = { Text("세부 정보") },
                            onClick = {
                                isMenuExpanded = false
                                sendIntent(PlaylistDetailListIntent.OnClickShowDetailAudioMedia(data))
                            },
                        )
                        DropdownMenuItem(
                            // 헤더의 "수정" 은 플레이리스트를 고친다. 이쪽은 곡이다.
                            text = { Text("음원 수정") },
                            onClick = {
                                isMenuExpanded = false
                                sendIntent(PlaylistDetailListIntent.OnClickEditAudioMedia(data))
                            },
                        )
                        DropdownMenuItem(
                            // 끌어서 바꾸는 쪽이 지름길이고, 이 항목이 그 보이는 경로다.
                            text = { Text("순서 조정") },
                            onClick = {
                                isMenuExpanded = false
                                sendIntent(PlaylistDetailListIntent.OnClickReorderAudioMedia(data))
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("삭제") },
                            colors = MenuDefaults.itemColors(textColor = softRed),
                            onClick = {
                                isMenuExpanded = false
                                isShowDeleteDialog = true
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview
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
