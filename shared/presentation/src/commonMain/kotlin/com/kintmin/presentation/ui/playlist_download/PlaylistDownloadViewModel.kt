package com.kintmin.presentation.ui.playlist_download

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.device.usecase.FetchConnectionStatusUseCase
import com.kintmin.domain.playlist_download.model.PlaylistDownloadFailureReason
import com.kintmin.domain.playlist_download.model.PlaylistDownloadPreview
import com.kintmin.domain.playlist_download.model.PlaylistDownloadProgress
import com.kintmin.domain.playlist_download.model.PlaylistDownloadStage
import com.kintmin.domain.playlist_download.model.PlaylistDownloadSummary
import com.kintmin.domain.playlist_download.usecase.DownloadYoutubePlaylistUseCase
import com.kintmin.domain.playlist_download.usecase.PreviewPlaylistDownloadUseCase
import com.kintmin.domain.youtube.model.YoutubePlaylist
import com.kintmin.presentation.port.BulkDownloadKeepAlive
import com.kintmin.presentation.port.RunningTaskMonitor
import com.kintmin.presentation.port.model.BulkDownloadTask
import com.kintmin.presentation.ui.playlist_download.navigation.PlaylistDownloadScreenRoute
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationFailureItem
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageItem
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageKind
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaylistDownloadViewModel(
    savedStateHandle: SavedStateHandle,
    private val previewPlaylistDownloadUseCase: PreviewPlaylistDownloadUseCase,
    private val downloadYoutubePlaylistUseCase: DownloadYoutubePlaylistUseCase,
    private val fetchConnectionStatusUseCase: FetchConnectionStatusUseCase,
    private val runningTaskMonitor: RunningTaskMonitor,
    private val bulkDownloadKeepAlive: BulkDownloadKeepAlive,
) : ViewModel() {

    private val url = savedStateHandle.toRoute<PlaylistDownloadScreenRoute>().url

    private val _uiState = MutableStateFlow(PlaylistDownloadUiState())
    val uiState: StateFlow<PlaylistDownloadUiState> = _uiState.asStateFlow()

    private val _eventFlow = Channel<PlaylistDownloadEvent>(Channel.BUFFERED)
    val eventFlow: Flow<PlaylistDownloadEvent> = _eventFlow.receiveAsFlow()

    private var playlist: YoutubePlaylist? = null
    private var runJob: Job? = null

    init {
        read()
    }

    fun sendIntent(intent: PlaylistDownloadIntent) {
        when (intent) {
            PlaylistDownloadIntent.OnClickStart -> start()
            PlaylistDownloadIntent.OnConfirmCellular -> run()
            PlaylistDownloadIntent.OnDismissCellular -> updatePreview { it.copy(askCellular = false) }
            PlaylistDownloadIntent.OnClickStop -> stop()
            // 기다리는 사이 목록과 연결이 바뀌었을 수 있어 다시 읽고 남은 곡과 Wi-Fi 를 다시 묻는다.
            PlaylistDownloadIntent.OnClickRetry -> read()
            PlaylistDownloadIntent.OnClickOpenPlaylist -> openPlaylist()
            PlaylistDownloadIntent.OnConfirmExit -> exit()
        }
    }

    private fun read() {
        updatePhase(PlaylistDownloadPhase.Reading)
        viewModelScope.launch {
            previewPlaylistDownloadUseCase(url)
                .onSuccess {
                    playlist = it.playlist
                    updatePhase(PlaylistDownloadPhase.Preview(it.toItem()))
                }
                .onFailure { updatePhase(PlaylistDownloadPhase.ReadFailed(readFailureMessage())) }
        }
    }

    /** Wi-Fi 가 아니면 막지 않고 한 번 묻는다. 연결 상태를 모르면 묻지 않는다. */
    private fun start() {
        val hasDownloads = ((_uiState.value.phase as? PlaylistDownloadPhase.Preview)?.preview?.downloadCount ?: 0) > 0
        val isWifi = fetchConnectionStatusUseCase().getOrNull()?.isWifi ?: true
        if (isWifi || !hasDownloads) run() else updatePreview { it.copy(askCellular = true) }
    }

    private fun run() {
        val playlist = playlist ?: return
        if (_uiState.value.isRunning) return
        _uiState.update {
            it.copy(
                phase = PlaylistDownloadPhase.Running,
                stages = PlaylistDownloadUiState.STAGES.map { kind -> DataMigrationStageItem(kind, DataMigrationStageStatus.PENDING) },
            )
        }
        runJob = viewModelScope.launch {
            // 다른 받기와 겹치면 같은 곡을 두 번 받거나 곡이 다른 재생목록으로 간다.
            val runningTasks = runningTaskMonitor.runningTasks()
            if (runningTasks.isNotEmpty()) {
                updatePhase(PlaylistDownloadPhase.Blocked(runningTasks))
                return@launch
            }
            bulkDownloadKeepAlive.keepAlive(BulkDownloadTask.PLAYLIST_DOWNLOAD) {
                downloadYoutubePlaylistUseCase(playlist) { updateStage(it.toStageItem()) }
            }
                .onSuccess { finish(it) }
                .onFailure { interrupt(downloadFailureMessage()) }
        }
    }

    private fun stop() {
        if (!_uiState.value.canStop) return
        _uiState.update { it.copy(isStopping = true) }
        viewModelScope.launch {
            runJob?.cancelAndJoin()
            // 담기를 마치고 끝난 결과는 덮지 않는다.
            if (_uiState.value.isRunning) interrupt(cause = null)
        }
    }

    /** 받기 단계는 실패한 곡도 끝난 곡으로 세며 진행했으므로, 끝나면 실제로 받은 곡 수로 바꾼다. */
    private fun finish(summary: PlaylistDownloadSummary) {
        val downloadFailed = summary.failures.any { it.reason != PlaylistDownloadFailureReason.UNAVAILABLE }
        _uiState.update { state ->
            state.copy(
                phase = PlaylistDownloadPhase.Finished(
                    playlistId = summary.playlistId,
                    playlistName = summary.playlistName,
                    downloadedCount = summary.downloadedCount,
                    alreadyOnDeviceCount = summary.alreadyOnDeviceCount,
                    failures = summary.failures.map { DataMigrationFailureItem(it.title, it.reason.toText()) },
                    hasRetryableFailures = summary.failures.any { it.reason == PlaylistDownloadFailureReason.DOWNLOAD_FAILED },
                ),
                stages = state.stages.map {
                    when {
                        it.kind != DataMigrationStageKind.REDOWNLOAD -> it.copy(status = DataMigrationStageStatus.DONE)
                        downloadFailed -> it.copy(status = DataMigrationStageStatus.FAILED, doneCount = summary.downloadedCount)
                        else -> it.copy(status = DataMigrationStageStatus.DONE)
                    }
                },
            )
        }
    }

    /** 멈춰도 받은 곡은 담고 끝나므로, 담기 단계가 보고한 수로 무엇이 남았는지 알린다. */
    private fun interrupt(cause: String?) {
        _uiState.update { state ->
            val addedCount = state.stages.firstOrNull { it.kind == DataMigrationStageKind.APPLY }?.totalCount ?: 0
            val next = if (addedCount > 0) {
                "받은 곡은 '${playlist?.title?.trim().orEmpty()}' 재생목록에 담았습니다. 다시 받으면 빠진 곡만 받아 제자리에 끼웁니다."
            } else {
                "다시 받으면 빠진 곡만 받습니다."
            }
            state.copy(
                phase = PlaylistDownloadPhase.Interrupted(listOfNotNull(cause, next).joinToString(" ")),
                isStopping = false,
                stages = state.stages.map {
                    when {
                        it.kind == DataMigrationStageKind.APPLY ->
                            it.copy(status = if (addedCount > 0) DataMigrationStageStatus.DONE else DataMigrationStageStatus.PENDING)
                        it.status != DataMigrationStageStatus.PENDING -> it.copy(status = DataMigrationStageStatus.FAILED)
                        else -> it
                    }
                },
            )
        }
    }

    private fun openPlaylist() {
        val playlistId = (_uiState.value.phase as? PlaylistDownloadPhase.Finished)?.playlistId ?: return
        viewModelScope.launch { _eventFlow.send(PlaylistDownloadEvent.NavigateToPlaylist(playlistId)) }
    }

    /** 받던 곡이 끝나기를 기다리지 않고 나간다. 받은 곡을 담는 정리는 취소되지 않고 마저 돈다. */
    private fun exit() {
        runJob?.cancel()
        viewModelScope.launch { _eventFlow.send(PlaylistDownloadEvent.NavigateToBack) }
    }

    private fun isDisconnected() = fetchConnectionStatusUseCase().getOrNull()?.isConnected == false

    private fun readFailureMessage(): String =
        if (isDisconnected()) {
            "인터넷 연결이 끊겼습니다. 연결한 뒤 다시 시도해 주세요."
        } else {
            "비공개 재생목록이면 공개나 일부 공개로 바꾼 뒤 다시 시도해 주세요."
        }

    /** 원인을 하나로 짚을 수 있는 것은 연결뿐이다. 나머지는 앱 로그로 보낸다. */
    private fun downloadFailureMessage(): String =
        if (isDisconnected()) {
            "인터넷 연결이 끊겼습니다."
        } else {
            "곡을 받다가 문제가 생겼습니다. 계속되면 설정 › 앱 로그 보기에서 원인을 확인할 수 있습니다."
        }

    /** 이번 단계보다 앞선 단계는 끝난 것이다. */
    private fun updateStage(item: DataMigrationStageItem) {
        _uiState.update { state ->
            state.copy(
                stages = state.stages.map {
                    when {
                        it.kind == item.kind -> item
                        it.kind.ordinal < item.kind.ordinal -> it.copy(status = DataMigrationStageStatus.DONE)
                        else -> it
                    }
                },
            )
        }
    }

    private fun updatePhase(phase: PlaylistDownloadPhase) {
        _uiState.update { it.copy(phase = phase) }
    }

    private fun updatePreview(transform: (PlaylistDownloadPhase.Preview) -> PlaylistDownloadPhase.Preview) {
        _uiState.update { state ->
            val phase = state.phase as? PlaylistDownloadPhase.Preview ?: return@update state
            state.copy(phase = transform(phase))
        }
    }

    private fun PlaylistDownloadPreview.toItem() = PlaylistDownloadPreviewItem(
        title = playlist.title.trim(),
        totalCount = totalCount,
        alreadyOnDeviceCount = alreadyOnDeviceCount,
        downloadCount = downloadCount,
        unavailableCount = unavailableCount,
        estimatedMegabytes = estimatedDownloadBytes / MEGABYTE,
        mergesIntoExistingPlaylist = mergesIntoExistingPlaylist,
    )

    private fun PlaylistDownloadProgress.toStageItem() = DataMigrationStageItem(
        kind = when (stage) {
            PlaylistDownloadStage.DOWNLOAD -> DataMigrationStageKind.REDOWNLOAD
            PlaylistDownloadStage.ADD_TO_PLAYLIST -> DataMigrationStageKind.APPLY
        },
        status = DataMigrationStageStatus.IN_PROGRESS,
        doneCount = doneCount,
        totalCount = totalCount,
    )

    private fun PlaylistDownloadFailureReason.toText(): String = when (this) {
        PlaylistDownloadFailureReason.UNAVAILABLE -> "비공개이거나 삭제된 영상"
        PlaylistDownloadFailureReason.DOWNLOAD_FAILED -> "받지 못함"
        PlaylistDownloadFailureReason.AGE_RESTRICTED -> "연령 제한 영상이라 로그인 없이 받을 수 없음"
    }

    private companion object {
        const val MEGABYTE = 1_000_000L
    }
}
