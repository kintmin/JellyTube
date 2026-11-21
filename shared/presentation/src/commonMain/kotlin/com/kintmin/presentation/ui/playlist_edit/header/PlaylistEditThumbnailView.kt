package com.kintmin.presentation.ui.playlist_edit.header

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.theme.overlayContent
import com.kintmin.presentation.theme.overlayScrim
import com.kintmin.presentation.ui.common.AudioArtwork
import com.kintmin.presentation.ui.common.JellyTubeDropdownMenu

@Composable
fun PlaylistEditThumbnailView(
    imageFileFullPath: String?,
    isCustomImage: Boolean,
    onClickPickImage: () -> Unit,
    onClickResetImage: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    AudioArtwork(
        imageFileFullPath = imageFileFullPath,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(8))
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                if (isCustomImage) isMenuExpanded = true else onClickPickImage()
            }
            .semantics { contentDescription = "썸네일 변경" },
        placeholderIconSize = 36.dp,
    ) {

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
                        color = overlayScrim,
                        shape = CircleShape,
                    )
                    .padding(6.dp),
                tint = overlayContent,
            )
            JellyTubeDropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
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
