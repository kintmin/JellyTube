package com.kintmin.presentation.ui.main.floating_action

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.kintmin.presentation.platform.rememberToaster
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.DownloadPlaylistSelectorBottomSheet

@Composable
fun MainFloatingActionButton(
    currentUrl: String,
    onClickDownload: (String) -> Unit,
) {
    val viewModel = koinViewModel<MainFloatingActionViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val toaster = rememberToaster()

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is MainFloatingActionEvent.Download -> onClickDownload(event.url)
                is MainFloatingActionEvent.ShowToast -> toaster.show(event.message)
            }
        }
    }

    MainFloatingActionButton(
        uiState = uiState,
        currentUrl = currentUrl,
        sendIntent = viewModel::sendIntent,
    )
}

/** 받을 재생목록 이름을 버튼에서 빼지 않는다. 숨기면 어디로 받는지 모른 채 누르게 된다. */
@Composable
fun MainFloatingActionButton(
    uiState: MainFloatingActionUiState,
    currentUrl: String,
    sendIntent: (MainFloatingActionIntent) -> Unit,
) {
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

    Surface(
        modifier = Modifier.padding(bottom = 48.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(role = Role.Button) {
                        sendIntent(MainFloatingActionIntent.OnClickDownload(currentUrl))
                    }
                    .padding(start = 16.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "다운로드",
                )
                Text(
                    modifier = Modifier.widthIn(max = 160.dp),
                    text = uiState.playlistIdOnDownloadName,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            VerticalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .clickable(role = Role.Button) {
                        sendIntent(MainFloatingActionIntent.OnClickPlaylistButton)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "받을 재생목록 바꾸기",
                )
            }
        }
    }
}

@Preview
@Composable
fun MainFloatingActionButtonPreview() {
    JellyTubeTheme {
        MainFloatingActionButton(
            uiState = MainFloatingActionUiState(
                playlistIdOnDownloadName = "미분류",
            ),
            currentUrl = "https://m.youtube.com/watch?v=test",
            sendIntent = {},
        )
    }
}
