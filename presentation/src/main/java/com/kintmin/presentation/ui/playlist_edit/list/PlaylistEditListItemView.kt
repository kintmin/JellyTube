package com.kintmin.presentation.ui.playlist_edit.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kintmin.presentation.theme.gray80
import java.io.File

@Composable
fun PlaylistEditListItemView(
    modifier: Modifier,
    data: PlaylistEditListItemUiState,
    draggingItemId: Int?,
    onDragStart: (Offset, Int) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    sendIntent: (PlaylistEditListIntent) -> Unit,
) {
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
        .clickable { sendIntent(PlaylistEditListIntent.OnClickEditCheck(data)) }
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
                    modifier = Modifier.size(24.dp).padding(4.dp),
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

        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .padding(8.dp)
                .clip(RoundedCornerShape(16))
                .background(gray80)
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
            onClick = {},
        ) {
            Icon(
                imageVector = Icons.Rounded.Reorder,
                contentDescription = "Reorder",
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PlaylistEditScreenPreview() {
    JellyTubeTheme {
        PlaylistEditListItemView(
            modifier = Modifier.height(56.dp),
            data = PlaylistEditListItemUiState.getMock(),
            draggingItemId = 1,
            onDragStart = { _, _ -> },
            onDrag = { _, _ -> },
            onDragEnd = {},
            sendIntent = {},
        )
    }
}