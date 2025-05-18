package com.kintmin.presentation.ui.playlist_edit.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.gray40
import com.kintmin.presentation.ui.playlist_edit.list.PlaylistEditListIntent
import java.io.File

@Composable
fun PlaylistEditHeaderView(
    data: PlaylistEditHeaderUiState,
    sendIntent: (PlaylistEditListIntent) -> Unit,
) {
    var titleText by remember { mutableStateOf(data.name) }
    var descriptionText by remember { mutableStateOf(data.description) }

    LaunchedEffect(data.name.isEmpty()) {
        titleText = data.name
        descriptionText = data.description
    }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp, 0.dp, 16.dp, 16.dp)
        ) {
            if (data.imageFileFullPath == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(220.dp)
                        .clip(RoundedCornerShape(8))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
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
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(220.dp)
                        .clip(RoundedCornerShape(8))
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(
                        width = 1.dp,
                        color = gray40,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp)),
                maxLines = 1,
                textStyle = TextStyle(),
                value = titleText,
                onValueChange = { newText ->
                    if (newText.length > 20) return@TextField
                    titleText = newText
                    if (newText.isNotEmpty()) {
                        sendIntent(PlaylistEditListIntent.OnEditPlaylistTitle(newText))
                    }
                },
                isError = titleText.isEmpty(),
                label = {
                    Text(
                        text = "제목 (${titleText.length}/20)",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                    )
                }
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(),
                value = descriptionText,
                onValueChange = { newText ->
                    if (newText.length > 100) return@OutlinedTextField
                    descriptionText = newText
                    sendIntent(PlaylistEditListIntent.OnEditPlaylistDescription(newText))
                },
                label = {
                    Text(
                        text = "설명 (${descriptionText.length}/100)",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistEditHeaderPreview() {
    JellyTubeTheme {
        PlaylistEditHeaderView(
            data = PlaylistEditHeaderUiState.getMock(),
            sendIntent = {},
        )
    }
}