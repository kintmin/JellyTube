package com.kintmin.presentation.ui.playlist_download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kintmin.presentation.platform.KeepScreenOnEffect
import com.kintmin.presentation.theme.JellyTubeTheme
import com.kintmin.presentation.ui.common.BottomActionBar
import com.kintmin.presentation.ui.common.BottomActionButton
import com.kintmin.presentation.ui.common.ConfirmStyle
import com.kintmin.presentation.ui.common.DetailTopAppBar
import com.kintmin.presentation.ui.common.EmptyStateView
import com.kintmin.presentation.ui.common.JellyTubeConfirmDialog
import com.kintmin.presentation.ui.playlist_download.view.PlaylistDownloadPreviewView
import com.kintmin.presentation.ui.playlist_download.view.PlaylistDownloadResultView
import com.kintmin.presentation.ui.setting.data_migration_receive.view.DataMigrationStageListView
import com.kintmin.presentation.ui.setting.section.SettingWarningBannerView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlaylistDownloadScreen(
    navigateToBack: () -> Unit,
    navigateToPlaylist: (playlistId: Int) -> Unit,
) {
    val viewModel = koinViewModel<PlaylistDownloadViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                PlaylistDownloadEvent.NavigateToBack -> navigateToBack()
                is PlaylistDownloadEvent.NavigateToPlaylist -> navigateToPlaylist(event.playlistId)
            }
        }
    }

    PlaylistDownloadScreen(
        uiState = uiState,
        navigateToBack = navigateToBack,
        sendIntent = viewModel::sendIntent,
    )
}

@Composable
fun PlaylistDownloadScreen(
    uiState: PlaylistDownloadUiState,
    navigateToBack: () -> Unit,
    sendIntent: (PlaylistDownloadIntent) -> Unit,
) {
    KeepScreenOnEffect(enabled = uiState.isRunning)

    var isExitDialogVisible by remember { mutableStateOf(false) }
    val onBack = { if (uiState.isRunning) isExitDialogVisible = true else navigateToBack() }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = uiState.isRunning,
    ) {
        isExitDialogVisible = true
    }
    if (uiState.isRunning) {
        JellyTubeConfirmDialog(
            showDialog = isExitDialogVisible,
            title = "받기를 멈추고 나갈까요?",
            message = "그때까지 받은 곡은 재생목록에 담고 멈춥니다. 같은 재생목록을 다시 받으면 빠진 곡만 받습니다.",
            dismissText = "계속 받기",
            confirmText = "멈추고 나가기",
            confirmStyle = ConfirmStyle.Destructive,
            onDismiss = { isExitDialogVisible = false },
            onConfirm = { sendIntent(PlaylistDownloadIntent.OnConfirmExit) },
        )
    }
    (uiState.phase as? PlaylistDownloadPhase.Preview)?.let { preview ->
        JellyTubeConfirmDialog(
            showDialog = preview.askCellular,
            title = "Wi-Fi에 연결되어 있지 않습니다",
            message = "곡 ${preview.preview.downloadCount}개를 받아 약 ${preview.preview.estimatedMegabytes} MB의 모바일 데이터를 씁니다.",
            confirmText = "모바일 데이터로 받기",
            onDismiss = { sendIntent(PlaylistDownloadIntent.OnDismissCellular) },
            onConfirm = { sendIntent(PlaylistDownloadIntent.OnConfirmCellular) },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { DetailTopAppBar(title = "재생목록 받기", onClickBack = onBack) },
        bottomBar = {
            uiState.bottomAction()?.let { action ->
                BottomActionBar {
                    BottomActionButton(text = action.text, enabled = action.enabled, onClick = { sendIntent(action.intent) })
                }
            }
        },
    ) { innerPadding ->
        when (val phase = uiState.phase) {
            PlaylistDownloadPhase.Reading -> {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(modifier = Modifier.padding(top = 12.dp), text = "재생목록을 읽고 있습니다...")
                    }
                }
                return@Scaffold
            }
            is PlaylistDownloadPhase.ReadFailed -> {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(icon = Icons.Rounded.ErrorOutline, title = "재생목록을 읽지 못했습니다", description = phase.message)
                }
                return@Scaffold
            }
            else -> Unit
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.isRunning) {
                SettingWarningBannerView(
                    title = "화면을 켜 두세요",
                    message = "앱을 나가면 받기가 멈출 수 있습니다. 멈추면 '다시 받기'로 받은 곡부터 이어 받습니다.",
                )
            }
            when (val phase = uiState.phase) {
                PlaylistDownloadPhase.Reading, is PlaylistDownloadPhase.ReadFailed -> Unit
                is PlaylistDownloadPhase.Blocked -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("아직 끝나지 않은 받기가 있습니다. 모두 끝난 뒤에 받을 수 있습니다.", style = MaterialTheme.typography.bodyMedium)
                    phase.runningTasks.forEach { (task, count) ->
                        Text("· ${task.label} ${count}건", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is PlaylistDownloadPhase.Preview -> PlaylistDownloadPreviewView(preview = phase.preview)
                PlaylistDownloadPhase.Running -> {
                    DataMigrationStageListView(stages = uiState.stages, label = PlaylistDownloadUiState::stageLabel)
                    if (uiState.isStopping) {
                        Text(
                            text = "받던 곡까지 마치고 멈추는 중입니다. 받은 곡은 재생목록에 담습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is PlaylistDownloadPhase.Interrupted, is PlaylistDownloadPhase.Finished -> {
                    DataMigrationStageListView(stages = uiState.stages, label = PlaylistDownloadUiState::stageLabel)
                    PlaylistDownloadResultView(
                        phase = phase,
                        onClickRetry = { sendIntent(PlaylistDownloadIntent.OnClickRetry) },
                    )
                }
            }
        }
    }
}

private class BottomAction(val text: String, val intent: PlaylistDownloadIntent, val enabled: Boolean = true)

/** 단계마다 다음에 할 일 하나를 화면 아래 같은 자리에 둔다. */
private fun PlaylistDownloadUiState.bottomAction(): BottomAction? = when (val phase = phase) {
    PlaylistDownloadPhase.Reading -> null
    is PlaylistDownloadPhase.ReadFailed -> BottomAction("다시 읽기", PlaylistDownloadIntent.OnClickRetry)
    is PlaylistDownloadPhase.Blocked -> BottomAction("다시 확인", PlaylistDownloadIntent.OnClickRetry)
    is PlaylistDownloadPhase.Preview -> BottomAction(
        text = if (phase.preview.downloadCount > 0) "${phase.preview.downloadCount}곡 받기" else "재생목록에 담기",
        intent = PlaylistDownloadIntent.OnClickStart,
        enabled = phase.preview.hasAnythingToAdd,
    )
    PlaylistDownloadPhase.Running -> BottomAction(
        text = if (isStopping) "멈추는 중…" else "멈추기",
        intent = PlaylistDownloadIntent.OnClickStop,
        enabled = canStop,
    )
    is PlaylistDownloadPhase.Interrupted -> BottomAction("다시 받기", PlaylistDownloadIntent.OnClickRetry)
    is PlaylistDownloadPhase.Finished -> phase.playlistId?.let { BottomAction("재생목록 열기", PlaylistDownloadIntent.OnClickOpenPlaylist) }
}

@Preview
@Composable
private fun PlaylistDownloadScreenPreview() {
    JellyTubeTheme {
        PlaylistDownloadScreen(uiState = PlaylistDownloadUiState.getMock(), navigateToBack = {}, sendIntent = {})
    }
}

@Preview
@Composable
private fun PlaylistDownloadScreenRunningPreview() {
    JellyTubeTheme {
        PlaylistDownloadScreen(uiState = PlaylistDownloadUiState.getRunningMock(), navigateToBack = {}, sendIntent = {})
    }
}
