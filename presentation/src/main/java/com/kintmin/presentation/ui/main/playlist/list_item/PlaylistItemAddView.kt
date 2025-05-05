package com.kintmin.presentation.ui.main.playlist.list_item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.main.playlist.PlaylistIntent
import com.kintmin.presentation.ui.main.playlist.dialog.PlaylistAddDialog

@Composable
fun PlaylistItemAddView(
    modifier: Modifier,
    sendIntent: (PlaylistIntent) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    PlaylistAddDialog(
        showDialog = showDialog,
        onDismiss = { showDialog = false },
        makePlaylist = { title ->
            sendIntent(PlaylistIntent.MakeNewPlaylist(title))
        },
    )

    Column(
        modifier = modifier.padding(12.dp).clickable {
            showDialog = true
        },
    ) {
        Box {
            IconButton(
                onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.Center)
                    .aspectRatio(1f)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(4))
                    .background(Color.Gray)
            ) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add"
                )
            }
        }
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            text = "재생목록 추가하기",
            fontSize = 16.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistItemAddPreview() {
    JellyTubeTheme {
        PlaylistItemAddView(
            modifier = Modifier.width(180.dp),
            sendIntent = {}
        )
    }
}