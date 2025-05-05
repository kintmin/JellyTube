package com.kintmin.presentation.ui.playlist_edit.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import java.io.File

@Composable
fun PlaylistEditHeaderView(
    data: PlaylistEditHeaderUiState,
) {
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(
            data.imageFileFullPath?.let { File(it) }
                ?: androidx.media3.session.R.drawable.media3_icon_artist
        )
        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
        .build()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp, 0.dp, 16.dp, 16.dp)
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(220.dp)
                .clip(RoundedCornerShape(8))
                .background(Color.Gray)
        )
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(
                    width = 1.dp,
                    color = Color.Gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
            ,
            maxLines = 1,
            textStyle = TextStyle(),
            value = data.name,
            onValueChange = { newText ->

            },
            label = {
                Text(
                    text = "제목",
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                )
            }
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
            ,
            maxLines = 3,
            textStyle = TextStyle(),
            value = data.description,
            onValueChange = { newText ->

            },
            label = {
                Text(
                    text = "설명",
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistEditHeaderPreview() {
    JellyTubeTheme {
        PlaylistEditHeaderView(
            data = PlaylistEditHeaderUiState.getMock(),
        )
    }
}