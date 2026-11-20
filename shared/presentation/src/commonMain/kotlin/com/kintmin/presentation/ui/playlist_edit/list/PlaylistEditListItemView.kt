package com.kintmin.presentation.ui.playlist_edit.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import okio.Path.Companion.toPath

@Composable
fun PlaylistEditListItemView(
    modifier: Modifier,
    data: PlaylistEditListItemUiState,
    dragHandleModifier: Modifier,
    sendIntent: (PlaylistEditListIntent) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { sendIntent(PlaylistEditListIntent.OnClickEditCheck(data)) },
    ) {
        IconButton(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterVertically),
            onClick = { sendIntent(PlaylistEditListIntent.OnClickEditCheck(data)) }
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (data.isChecked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    ),
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp),
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Check",
                    tint = if (data.isChecked) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                    },
                )
            }
        }

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
                    .background(MaterialTheme.colorScheme.surface),
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

        // 눌러도 아무 일이 없는 IconButton 이 아니라, 끌기만 받는 자리로 둔다.
        Icon(
            modifier = dragHandleModifier
                .align(Alignment.CenterVertically)
                .size(48.dp)
                .padding(12.dp),
            imageVector = Icons.Rounded.Reorder,
            contentDescription = "끌어서 순서 변경",
        )
    }
}


@Preview
@Composable
fun PlaylistEditScreenPreview() {
    JellyTubeTheme {
        PlaylistEditListItemView(
            modifier = Modifier.height(56.dp),
            data = PlaylistEditListItemUiState.getMock(),
            dragHandleModifier = Modifier,
            sendIntent = {},
        )
    }
}