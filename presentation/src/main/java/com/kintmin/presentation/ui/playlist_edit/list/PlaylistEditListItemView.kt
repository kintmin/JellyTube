package com.kintmin.presentation.ui.playlist_edit.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListIntent
import com.kintmin.presentation.ui.playlist_detail.list.PlaylistDetailListItemUiState
import java.io.File

@Composable
fun PlaylistEditListItemView(
    data: PlaylistDetailListItemUiState,
) {
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(
            data.imageFileFullPath?.let { File(it) }
                ?: androidx.media3.session.R.drawable.media3_icon_artist
        )
        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
        .build()

    Row(modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .clickable {
        }
    ) {
        IconButton(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterVertically),
            onClick = {  }
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delete",
                tint = Color(0xFFEE1111)
            )
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

        IconButton(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterVertically),
            onClick = {  }
        ) {
            Icon(
                imageVector = Icons.Rounded.Reorder,
                contentDescription = "Reorder"
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PlaylistEditScreenPreview() {
    JellyTubeTheme {
        PlaylistEditListItemView(
            PlaylistDetailListItemUiState.getMock()
        )
    }
}