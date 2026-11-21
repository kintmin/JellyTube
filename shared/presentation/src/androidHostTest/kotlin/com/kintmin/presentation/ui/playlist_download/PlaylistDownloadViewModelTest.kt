package com.kintmin.presentation.ui.playlist_download

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.kintmin.domain.device.model.ConnectionStatus
import com.kintmin.domain.device.usecase.FetchConnectionStatusUseCase
import com.kintmin.domain.playlist_download.model.PlaylistDownloadFailure
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
import com.kintmin.presentation.port.model.RunningTask
import com.kintmin.presentation.ui.playlist_download.navigation.PlaylistDownloadScreenRoute
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageKind
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistDownloadViewModelTest {

    private val url = "https://m.youtube.com/playlist?list=PLx"
    private val playlist = YoutubePlaylist("드라이브", emptyList())
    private var preview = PlaylistDownloadPreview(playlist, 10, 2, 8, 0, 8_000_000, false)
    private val previewUseCase = mockk<PreviewPlaylistDownloadUseCase> {
        coEvery { this@mockk(url) } answers { Result.success(preview) }
    }
    private val download = mockk<DownloadYoutubePlaylistUseCase>()
    private var isWifi = true
    private val connection = mockk<FetchConnectionStatusUseCase> {
        every { this@mockk() } answers { Result.success(ConnectionStatus(true, isWifi = isWifi, isCellular = !isWifi, 0, 0)) }
    }
    private val monitor = mockk<RunningTaskMonitor> {
        coEvery { runningTasks() } returns emptyMap()
    }
    private val keptAlive = mutableListOf<BulkDownloadTask>()
    private val keepAlive = object : BulkDownloadKeepAlive {
        override suspend fun <T> keepAlive(task: BulkDownloadTask, block: suspend () -> T): T {
            keptAlive += task
            return block()
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every { any<SavedStateHandle>().toRoute(PlaylistDownloadScreenRoute::class, any()) } returns PlaylistDownloadScreenRoute(url)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
        Dispatchers.resetMain()
    }

    private fun viewModel() = PlaylistDownloadViewModel(SavedStateHandle(), previewUseCase, download, connection, monitor, keepAlive)

    private fun summary(failures: List<PlaylistDownloadFailure> = emptyList()) =
        PlaylistDownloadSummary(playlistId = 5, playlistName = "드라이브", downloadedCount = 8, alreadyOnDeviceCount = 2, failures = failures)

    @Test
    fun `화면을 열면 목록을 읽어 무엇을 받을지 보여 주고 받기는 누를 때만 한다`() {
        val phase = viewModel().uiState.value.phase as PlaylistDownloadPhase.Preview

        assertEquals(PlaylistDownloadPreviewItem("드라이브", 10, 2, 8, 0, 8, false), phase.preview)
        coVerify(exactly = 0) { download(any(), any()) }
    }

    @Test
    fun `Wi-Fi 가 아니면 받기 전에 묻고 받을 곡이 없으면 묻지 않는다`() {
        isWifi = false
        coEvery { download(playlist, any()) } returns Result.success(summary())
        val viewModel = viewModel()

        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStart)
        assertTrue((viewModel.uiState.value.phase as PlaylistDownloadPhase.Preview).askCellular)

        preview = preview.copy(downloadCount = 0)
        val nothingToDownload = viewModel()
        nothingToDownload.sendIntent(PlaylistDownloadIntent.OnClickStart)
        assertTrue(nothingToDownload.uiState.value.phase is PlaylistDownloadPhase.Finished)
    }

    @Test
    fun `다른 받기가 돌고 있으면 시작하지 않는다`() {
        coEvery { monitor.runningTasks() } returns mapOf(RunningTask.YOUTUBE_DOWNLOAD to 1)
        val viewModel = viewModel()

        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStart)

        assertEquals(PlaylistDownloadPhase.Blocked(mapOf(RunningTask.YOUTUBE_DOWNLOAD to 1)), viewModel.uiState.value.phase)
        coVerify(exactly = 0) { download(any(), any()) }
    }

    @Test
    fun `다시 확인하면 바로 받지 않고 목록을 다시 읽어 Wi-Fi 를 다시 묻는다`() {
        coEvery { monitor.runningTasks() } returns mapOf(RunningTask.YOUTUBE_DOWNLOAD to 1)
        val viewModel = viewModel()
        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStart)
        coEvery { monitor.runningTasks() } returns emptyMap()
        isWifi = false

        viewModel.sendIntent(PlaylistDownloadIntent.OnClickRetry)
        assertTrue(viewModel.uiState.value.phase is PlaylistDownloadPhase.Preview)
        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStart)

        assertTrue((viewModel.uiState.value.phase as PlaylistDownloadPhase.Preview).askCellular)
        coVerify(exactly = 2) { previewUseCase(url) }
        coVerify(exactly = 0) { download(any(), any()) }
    }

    @Test
    fun `받는 동안 앱을 살려 두고 끝나면 받지 못한 곡만 다시 받을 수 있다`() {
        coEvery { download(playlist, any()) } returns Result.success(
            summary(
                listOf(
                    PlaylistDownloadFailure("비공개", PlaylistDownloadFailureReason.UNAVAILABLE),
                    PlaylistDownloadFailure("밤편지", PlaylistDownloadFailureReason.DOWNLOAD_FAILED),
                ),
            ),
        )
        val viewModel = viewModel()

        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStart)

        val finished = viewModel.uiState.value.phase as PlaylistDownloadPhase.Finished
        assertTrue(finished.hasRetryableFailures)
        val download = viewModel.uiState.value.stages.single { it.kind == DataMigrationStageKind.REDOWNLOAD }
        assertEquals(DataMigrationStageStatus.FAILED, download.status)
        assertEquals(8, download.doneCount)
        assertEquals(listOf(BulkDownloadTask.PLAYLIST_DOWNLOAD), keptAlive)

        coEvery { download(playlist, any()) } returns Result.success(
            summary(listOf(PlaylistDownloadFailure("비공개", PlaylistDownloadFailureReason.UNAVAILABLE))),
        )
        viewModel.sendIntent(PlaylistDownloadIntent.OnClickRetry)
        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStart)
        assertFalse((viewModel.uiState.value.phase as PlaylistDownloadPhase.Finished).hasRetryableFailures)
        coVerify(exactly = 2) { previewUseCase(url) }
    }

    @Test
    fun `멈추면 받은 곡을 담은 재생목록을 알리고 끝나면 그 재생목록으로 갈 수 있다`() = runTest {
        val never = CompletableDeferred<Result<PlaylistDownloadSummary>>()
        coEvery { download(playlist, any()) } coAnswers {
            val onProgress = secondArg<(PlaylistDownloadProgress) -> Unit>()
            try {
                never.await()
            } finally {
                onProgress(PlaylistDownloadProgress(PlaylistDownloadStage.ADD_TO_PLAYLIST, 3, 3))
            }
        }
        val viewModel = viewModel()
        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStart)
        assertTrue(viewModel.uiState.value.canStop)

        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStop)
        val interrupted = viewModel.uiState.value.phase as PlaylistDownloadPhase.Interrupted
        assertTrue(interrupted.message.startsWith("받은 곡은 '드라이브' 재생목록에 담았습니다."))
        assertEquals(
            listOf(DataMigrationStageStatus.FAILED, DataMigrationStageStatus.DONE),
            viewModel.uiState.value.stages.map { it.status },
        )

        coEvery { download(playlist, any()) } returns Result.success(summary())
        viewModel.sendIntent(PlaylistDownloadIntent.OnClickRetry)
        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStart)
        viewModel.sendIntent(PlaylistDownloadIntent.OnClickOpenPlaylist)
        assertEquals(PlaylistDownloadEvent.NavigateToPlaylist(5), viewModel.eventFlow.first())
    }

    @Test
    fun `받던 곡이 끝나기를 기다리는 동안 멈추는 중임을 보이고 나가기는 기다리지 않는다`() = runTest {
        val inFlight = CompletableDeferred<Unit>()
        coEvery { download(playlist, any()) } coAnswers {
            withContext(NonCancellable) { inFlight.await() }
            awaitCancellation()
        }
        val viewModel = viewModel()
        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStart)

        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStop)
        assertTrue(viewModel.uiState.value.isStopping)
        assertFalse(viewModel.uiState.value.canStop)
        assertEquals(PlaylistDownloadPhase.Running, viewModel.uiState.value.phase)

        viewModel.sendIntent(PlaylistDownloadIntent.OnConfirmExit)
        assertEquals(PlaylistDownloadEvent.NavigateToBack, viewModel.eventFlow.first())

        inFlight.complete(Unit)
        assertTrue(viewModel.uiState.value.phase is PlaylistDownloadPhase.Interrupted)
        assertFalse(viewModel.uiState.value.isStopping)
    }

    @Test
    fun `멈추는 사이 담기까지 마쳐 끝났으면 멈췄다고 덮지 않는다`() = runTest {
        val inFlight = CompletableDeferred<Unit>()
        coEvery { download(playlist, any()) } coAnswers {
            withContext(NonCancellable) { inFlight.await() }
            Result.success(summary())
        }
        val viewModel = viewModel()
        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStart)

        viewModel.sendIntent(PlaylistDownloadIntent.OnClickStop)
        inFlight.complete(Unit)

        assertTrue(viewModel.uiState.value.phase is PlaylistDownloadPhase.Finished)
    }

    @Test
    fun `읽지 못하면 다시 읽을 수 있다`() {
        coEvery { previewUseCase(url) } returns Result.failure(IllegalStateException("private"))
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.phase is PlaylistDownloadPhase.ReadFailed)

        coEvery { previewUseCase(url) } answers { Result.success(preview) }
        viewModel.sendIntent(PlaylistDownloadIntent.OnClickRetry)

        assertTrue(viewModel.uiState.value.phase is PlaylistDownloadPhase.Preview)
    }
}
