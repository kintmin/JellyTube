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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.kintmin.presentation.animation.NowPlayingBars
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray60
import com.kintmin.presentation.theme.gray80
import com.kintmin.presentation.theme.softRed
import com.kintmin.presentation.ui.playlist_detail.dialog.RemoveAudioMediaFromPlaylistDialog
import okio.Path.Companion.toPath

@Composable
fun PlaylistDetailListItemView(
    modifier: Modifier,
    data: PlaylistDetailListItemUiState,
    canRemoveTrack: Boolean,
    isNowPlaying: Boolean,
    isPlaying: Boolean,
    isDragging: Boolean,
    sendIntent: (PlaylistDetailListIntent) -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isShowRemoveDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    // pointerInput 은 키만 비교하고 람다는 비교하지 않아, 아래 탭 처리가 첫 구성 시점의 값에
    // 묶인다.
    val isDraggingNow by rememberUpdatedState(isDragging)

    Card(
        modifier,
        shape = RectangleShape,
    ) {
        RemoveAudioMediaFromPlaylistDialog(
            showDialog = isShowRemoveDialog,
            onDismiss = { isShowRemoveDialog = false },
            mediaName = data.mediaName,
            removeFromPlaylist = {
                sendIntent(PlaylistDetailListIntent.OnClickRemoveFromPlaylist(data))
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
                        //
                        // 들린 뒤 **움직이지 않고** 떼면 여기로 온다 — onLongPress 가 없어 탭
                        // 감지가 끝나지 않고, 끌기가 좌표를 소비하기 전이라 취소도 되지 않는다.
                        onTap = {
                            if (!isDraggingNow) {
                                sendIntent(PlaylistDetailListIntent.OnClickAudioItem(data))
                            }
                        },
                    )
                },
        ) {
            Row {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (data.imageFileFullPath == null) {
                        Icon(
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(data.imageFileFullPath.toPath())
                                .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil3.request.CachePolicy.DISABLED)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }

                    if (isNowPlaying) {
                        // 임의의 앨범아트 위에서도 기둥이 읽히게 깔고 그린다.
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
                        )
                        // 기둥은 Canvas 라 스크린리더에 아무것도 남기지 않는다.
                        NowPlayingBars(
                            modifier = Modifier
                                .matchParentSize()
                                .semantics {
                                    contentDescription = if (isPlaying) "재생 중" else "일시정지"
                                },
                            isPlaying = isPlaying,
                        )
                    }
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
                            // 헤더의 "편집" 은 플레이리스트를 고친다. 이쪽은 곡이다.
                            text = { Text("음원 정보 수정") },
                            onClick = {
                                isMenuExpanded = false
                                sendIntent(PlaylistDetailListIntent.OnClickEditAudioMedia(data))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("플레이리스트 편집") },
                            onClick = {
                                isMenuExpanded = false
                                sendIntent(PlaylistDetailListIntent.OnClickEditPlaylist(data))
                            },
                        )
                        if (canRemoveTrack) {
                            DropdownMenuItem(
                                text = { Text("플레이리스트에서 빼기") },
                                colors = MenuDefaults.itemColors(textColor = softRed),
                                onClick = {
                                    isMenuExpanded = false
                                    isShowRemoveDialog = true
                                },
                            )
                        }
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
            modifier = Modifier.height(80.dp),
            canRemoveTrack = true,
            isNowPlaying = false,
            isPlaying = false,
            isDragging = false,
            sendIntent = {},
        )
    }
}

@Preview
@Composable
fun PlaylistDetailListItemNowPlayingPreview() {
    JellyTubeTheme {
        PlaylistDetailListItemView(
            data = PlaylistDetailListItemUiState.getMock(),
            modifier = Modifier.height(80.dp),
            canRemoveTrack = true,
            isNowPlaying = true,
            isPlaying = true,
            isDragging = false,
            sendIntent = {},
        )
    }
}
