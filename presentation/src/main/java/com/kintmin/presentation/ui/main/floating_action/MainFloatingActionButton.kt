package com.kintmin.presentation.ui.main.floating_action

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.DownloadPlaylistSelectorBottomSheet

@Composable
fun MainFloatingActionButton(
    currentUrl: String,
    onClickDownload: (String) -> Unit,
) {
    val viewModel = hiltViewModel<MainFloatingActionViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is MainFloatingActionEvent.Download -> onClickDownload(event.url)
                is MainFloatingActionEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    MainFloatingActionButton(
        uiState = uiState,
        currentUrl = currentUrl,
        sendIntent = viewModel::sendIntent,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainFloatingActionButton(
    uiState: MainFloatingActionUiState,
    currentUrl: String,
    sendIntent: (MainFloatingActionIntent) -> Unit,
) {
    var isPlaylistButtonVisible by rememberSaveable { mutableStateOf(false) }

    if (uiState.isPlaylistBottomSheetVisible) {
        DownloadPlaylistSelectorBottomSheet(
            playlistList = uiState.selectablePlaylistList,
            onSelectPlaylist = { playlistId ->
                sendIntent(MainFloatingActionIntent.OnSelectPlaylist(playlistId))
            },
            onDismissRequest = {
                sendIntent(MainFloatingActionIntent.OnDismissPlaylistBottomSheet)
            },
        )
    }

    Column(
        modifier = Modifier.padding(bottom = 48.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(
            visible = isPlaylistButtonVisible,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut(),
        ) {
            ExtendedFloatingActionButton(
                modifier = Modifier.alpha(0.9f),
                onClick = {
                    sendIntent(MainFloatingActionIntent.OnClickPlaylistButton)
                },
                shape = MaterialTheme.shapes.extraLarge,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                ),
            ) {
                Text(text = "현재 재생목록: ${uiState.playlistIdOnDownloadName}")
            }
        }

        Surface(
            modifier = Modifier
                .size(56.dp)
                .combinedClickable(
                    role = Role.Button,
                    onLongClick = {
                        isPlaylistButtonVisible = !isPlaylistButtonVisible
                    },
                    onClick = {
                        sendIntent(MainFloatingActionIntent.OnClickDownload(currentUrl))
                    },
                ),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 6.dp,
        ) {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "추가",
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainFloatingActionButtonPreview() {
    JellyTubeTheme {
        MainFloatingActionButton(
            uiState = MainFloatingActionUiState(
                playlistIdOnDownloadName = "기본",
            ),
            currentUrl = "https://m.youtube.com/watch?v=test",
            sendIntent = {},
        )
    }
}
