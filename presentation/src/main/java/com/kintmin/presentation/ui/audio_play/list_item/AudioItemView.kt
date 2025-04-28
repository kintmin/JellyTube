package com.kintmin.presentation.ui.audio_play.list_item

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import com.kintmin.presentation.ui.audio_play.AudioPlayIntent
import java.io.File

@Composable
fun AudioItemView(
    data: AudioPlayUiState,
    isBasePlaylist: Boolean,
    sendIntent: (AudioPlayIntent) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

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
            sendIntent(AudioPlayIntent.OnClickAudioItem(data))
        }
    ) {
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
                text = data.extraString,
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
                onClick = { expanded = true }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("정보 수정") },
                    onClick = {
                        expanded = false
                        // 타이틀 수정
                        // 설명 수정
                        // 아티스트 수정
                        // 이미지 수정
                        // 플레이리스트 수정 (다수 선택 가능)
                        // 음원 제거
                    }
                )
                if (!isBasePlaylist) {
                    DropdownMenuItem(
                        text = { Text("플레이리스트에서 제거") },
                        onClick = {
                            expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("음원 제거") },
                    onClick = {
                        sendIntent(AudioPlayIntent.OnClickDeleteAudioMedia(data))
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AudioItemPreview() {
    JellyTubeTheme {
        AudioItemView(
            data = AudioPlayUiState.getMock(),
            isBasePlaylist = true,
            sendIntent = {},
        )
    }
}