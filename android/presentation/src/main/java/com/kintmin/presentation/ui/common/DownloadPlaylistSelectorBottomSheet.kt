package com.kintmin.presentation.ui.common

import android.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.kintmin.presentation.theme.JellyTubeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadPlaylistSelectorBottomSheet(
    playlistList: List<DownloadPlaylistUiState>,
    onSelectPlaylist: (playlistId: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        KeepBottomSheetNavigationBarDark()

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(
                items = playlistList,
                key = { item -> item.id },
            ) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable {
                            onSelectPlaylist(item.id)
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.name,
                        color = if (item.isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun KeepBottomSheetNavigationBarDark() {
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect {
        dialogWindow?.let { window ->
            window.navigationBarColor = Color.BLACK
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightNavigationBars = false
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadPlaylistSelectorBottomSheetPreview() {
    JellyTubeTheme {
        DownloadPlaylistSelectorBottomSheet(
            playlistList = listOf(
                DownloadPlaylistUiState(
                    id = 1,
                    name = "기본 재생목록",
                    isSelected = true,
                ),
                DownloadPlaylistUiState(
                    id = 2,
                    name = "운동할 때 듣는 음악",
                    isSelected = false,
                ),
                DownloadPlaylistUiState(
                    id = 3,
                    name = "출퇴근 플레이리스트",
                    isSelected = false,
                ),
            ),
            onSelectPlaylist = {},
            onDismissRequest = {},
        )
    }
}

data class DownloadPlaylistUiState(
    val id: Int,
    val name: String,
    val isSelected: Boolean,
)
