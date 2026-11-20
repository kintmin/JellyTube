package com.kintmin.presentation.ui.playlist_edit.header

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.white
import okio.Path.Companion.toPath

/** 고른 사진이 없으면 바로 선택기를 열고, 있으면 기본 썸네일로 되돌릴지도 묻는다. */
@Composable
fun PlaylistEditThumbnailView(
    imageFileFullPath: String?,
    isCustomImage: Boolean,
    onClickPickImage: () -> Unit,
    onClickResetImage: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(8))
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                if (isCustomImage) isMenuExpanded = true else onClickPickImage()
            }
            .semantics { contentDescription = "썸네일 변경" },
        contentAlignment = Alignment.Center,
    ) {
        if (imageFileFullPath == null) {
            Icon(
                imageVector = Icons.Rounded.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(imageFileFullPath.toPath())
                    .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil3.request.CachePolicy.DISABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                        shape = CircleShape,
                    )
                    .padding(6.dp),
                tint = white,
            )
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                // M3 기본 메뉴 모서리는 extraSmall(4dp) 이라 각져 보인다.
                shape = MaterialTheme.shapes.medium,
            ) {
                DropdownMenuItem(
                    text = { Text("사진 선택") },
                    onClick = {
                        isMenuExpanded = false
                        onClickPickImage()
                    },
                )
                DropdownMenuItem(
                    text = { Text("기본 썸네일로 되돌리기") },
                    onClick = {
                        isMenuExpanded = false
                        onClickResetImage()
                    },
                )
            }
        }
    }
}

@Preview
@Composable
fun PlaylistEditThumbnailViewPreview() {
    JellyTubeTheme {
        PlaylistEditThumbnailView(
            imageFileFullPath = null,
            isCustomImage = false,
            onClickPickImage = {},
            onClickResetImage = {},
        )
    }
}
