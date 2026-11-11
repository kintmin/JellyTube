package com.kintmin.domain.data_migration

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.audio_media.model.DownloadedAudioFile
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.data_migration.model.MigrationApplyResult
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFailure
import com.kintmin.domain.data_migration.model.MigrationFailureReason
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationFileUnavailableException
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationProgress
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationStage
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.domain.data_migration.usecase.ImportMigrationDataUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportMigrationDataUseCaseTest {

    private val endpoint = MigrationEndpoint("192.168.0.2", 52847)
    private val playlistRepository = mockk<PlaylistRepository> {
        coEvery { ensureSystemPlaylists() } returns Result.success(Unit)
    }
    private val appSettingRepository = mockk<AppSettingRepository> {
        coEvery { updateShouldInsertAtTopOnDownload(any()) } returns Result.success(Unit)
        coEvery { updatePlaylistIdOnDownload(any()) } returns Result.success(Unit)
    }
    private val audioMediaRepository = mockk<AudioMediaRepository> {
        coEvery { deleteFileAtFullPath(any()) } returns Result.success(Unit)
    }
    private val appLog = mockk<AppLog>(relaxed = true)
    private val targetSystem = listOf(
        MigrationPlaylist(1, "전체", "", null, 0, PlaylistType.TOTAL, 0),
        MigrationPlaylist(2, "미분류", "", null, 0, PlaylistType.UNCATEGORIZED, 0),
        MigrationPlaylist(3, "즐겨찾기", "", null, 0, PlaylistType.FAVORITE, 0),
    )
    private val emptyTarget = MigrationSnapshot(emptyList(), targetSystem, emptyList(), emptyList(), emptyList())

    private fun audio(id: Int, source: String, audioFile: String, edited: String? = null) = MigrationAudioMedia(
        id = id, source = source, name = "곡$id", artist = "", description = "", rawAudioDurationSeconds = 1,
        audioFileName = audioFile, imageFileName = null, lyricFileName = null, tjKaraokeNumber = null,
        editedAudioFileName = edited, editedAudioDurationSeconds = edited?.let { 1 }, createdTimeMillis = id.toLong(),
    )

    private fun entry(name: String, size: Long = 10) =
        MigrationFileEntry(MigrationFile(MigrationFileKind.AUDIO, name), size)

    private fun export(
        audios: List<MigrationAudioMedia>,
        files: List<MigrationFileEntry>,
        playlists: List<MigrationPlaylist> = emptyList(),
        settings: MigrationDownloadSettings? = null,
    ) = MigrationExport(
        snapshot = MigrationSnapshot(audios, playlists, emptyList(), emptyList(), emptyList()),
        files = files,
        downloadSettings = settings,
    )

    private fun useCase(repository: DataMigrationRepository) =
        ImportMigrationDataUseCase(repository, playlistRepository, appSettingRepository, audioMediaRepository, appLog)

    private class FakeRepository(
        private val export: MigrationExport,
        private val target: MigrationSnapshot,
        /** 이 이름의 파일은 그 파일만 받을 수 없다. */
        private val unavailableFiles: Set<String> = emptySet(),
        /** 이 이름의 파일을 받다가 연결이 끊긴다. */
        private val disconnectAt: String? = null,
        private val failClearStaging: Boolean = false,
        /** 정하면 이 크기로 잘라 여러 번 받은 것처럼 알린다. */
        private val chunkSize: Long? = null,
    ) : DataMigrationRepository {
        val calls = mutableListOf<String>()
        val staged = mutableListOf<String>()
        var appliedPlan: MigrationPlan? = null

        override suspend fun exportData() = error("받는 쪽에서 부르지 않는다")
        override fun resolveFilePath(file: MigrationFile) = error("받는 쪽에서 부르지 않는다")
        override suspend fun fetchSnapshot() = Result.success(target).also { calls += "fetchSnapshot" }
        override fun isPlayableAudioFile(fileName: String) = !fileName.endsWith(".webm")
        override suspend fun pair(endpoint: MigrationEndpoint, code: String) = error("쓰지 않는다")
        override suspend fun fetchRemoteExport(endpoint: MigrationEndpoint, token: String) =
            Result.success(export).also { calls += "fetchRemoteExport" }

        override suspend fun stageFile(
            endpoint: MigrationEndpoint,
            token: String,
            entry: MigrationFileEntry,
            onReceived: (Long) -> Unit,
        ): Result<Unit> {
            calls += "stage"
            val name = entry.file.name
            if (name == disconnectAt) return Result.failure(IllegalStateException("끊김"))
            if (name in unavailableFiles) return Result.failure(MigrationFileUnavailableException(name))
            staged += name
            if (chunkSize == null) {
                onReceived(entry.size / 2)
            } else {
                (chunkSize..entry.size step chunkSize).forEach(onReceived)
            }
            return Result.success(Unit)
        }

        override suspend fun applyPlan(plan: MigrationPlan) =
            Result.success(MigrationApplyResult(plan.newPlaylists.associate { it.id to it.id + 1000 })).also {
                calls += "apply"
                appliedPlan = plan
            }

        override suspend fun clearStaging(): Result<Unit> {
            calls += "clear"
            return if (failClearStaging) Result.failure(IllegalStateException("디스크 오류")) else Result.success(Unit)
        }
        override suspend fun notifyFinished(endpoint: MigrationEndpoint, token: String, summary: MigrationSummary) =
            Result.success(Unit).also { calls += "finish" }
    }

    @Test
    fun `새 곡의 파일만 받아 둔 뒤 반영하고 단계를 순서대로 알린다`() = runTest {
        val repository = FakeRepository(
            export = export(
                audios = listOf(audio(101, "https://youtu.be/new", "new.m4a"), audio(102, "https://youtu.be/old", "old.m4a")),
                files = listOf(entry("new.m4a", 100), entry("old.m4a", 50)),
            ),
            target = emptyTarget.copy(audioMedias = listOf(audio(7, "https://www.youtube.com/watch?v=old", "mine.m4a"))),
        )
        val progress = mutableListOf<MigrationProgress>()

        val summary = useCase(repository)(endpoint, "t", progress::add).getOrThrow()

        assertEquals(listOf("new.m4a"), repository.staged)
        assertEquals(listOf("fetchRemoteExport", "fetchSnapshot", "stage", "apply", "clear", "finish"), repository.calls)
        assertEquals(listOf(101), repository.appliedPlan?.newAudioMedias?.map { it.id })
        assertEquals(1, summary.newAudioMediaCount)
        assertTrue(summary.failures.isEmpty())
        assertEquals(
            listOf(MigrationStage.PLANNING, MigrationStage.REDOWNLOAD, MigrationStage.TRANSFER, MigrationStage.APPLY),
            progress.map { it.stage }.distinct(),
        )
        assertTrue(MigrationProgress(MigrationStage.TRANSFER, 0, 1, 50, 100) in progress)
        assertTrue(MigrationProgress(MigrationStage.TRANSFER, 1, 1, 100, 100) in progress)
        coVerify(exactly = 1) { playlistRepository.ensureSystemPlaylists() }
    }

    @Test
    fun `파일 받기 진행은 청크마다가 아니라 1퍼센트 단위로 알린다`() = runTest {
        val repository = FakeRepository(
            export = export(listOf(audio(101, "s1", "a.m4a")), listOf(entry("a.m4a", size = 10_000))),
            target = emptyTarget,
            chunkSize = 1,
        )
        val progress = mutableListOf<MigrationProgress>()

        useCase(repository)(endpoint, "t", progress::add).getOrThrow()

        val transfer = progress.filter { it.stage == MigrationStage.TRANSFER }
        assertTrue("${transfer.size}", transfer.size <= 102)
        assertEquals(MigrationProgress(MigrationStage.TRANSFER, 1, 1, 10_000, 10_000), transfer.last())
    }

    @Test
    fun `보내는 기기에 음원 파일이 없는 새 곡은 실패로 알리고 이미 있는 곡은 알리지 않는다`() = runTest {
        val repository = FakeRepository(
            export = export(
                audios = listOf(
                    audio(101, "s1", "../evil.m4a"),
                    audio(102, "s2", "missing.m4a"),
                    audio(103, "https://youtu.be/mine", "gone.m4a"),
                ),
                files = listOf(entry("../evil.m4a")),
            ),
            target = emptyTarget.copy(audioMedias = listOf(audio(7, "https://youtu.be/mine", "mine.m4a"))),
        )

        val summary = useCase(repository)(endpoint, "t") {}.getOrThrow()

        assertTrue(repository.staged.isEmpty())
        assertTrue(repository.appliedPlan!!.newAudioMedias.isEmpty())
        assertEquals(
            listOf(
                MigrationFailure("곡101", MigrationFailureReason.TRANSFER_FAILED),
                MigrationFailure("곡102", MigrationFailureReason.TRANSFER_FAILED),
            ),
            summary.failures,
        )
    }

    @Test
    fun `재생할 수 없는 유튜브 곡은 다시 받고 그 밖의 곡은 실패 사유와 함께 뺀다`() = runTest {
        coEvery { audioMediaRepository.downloadAudioFile("https://youtu.be/yt") } returns
            Result.success(DownloadedAudioFile("re.m4a", "/audios/re.m4a", durationSeconds = 42))
        coEvery { audioMediaRepository.downloadAudioFile("https://youtu.be/fail") } returns
            Result.failure(IllegalStateException("m4a 없음"))
        val repository = FakeRepository(
            export = export(
                audios = listOf(
                    audio(101, "https://youtu.be/yt", "yt.webm"),
                    audio(102, "fileShare://sha256/x", "x.webm"),
                    audio(103, "https://youtu.be/fail", "fail.webm"),
                    audio(104, "https://youtu.be/ok", "ok.m4a"),
                ),
                files = listOf(entry("yt.webm"), entry("x.webm"), entry("fail.webm"), entry("ok.m4a")),
            ),
            target = emptyTarget,
        )
        val progress = mutableListOf<MigrationProgress>()

        val summary = useCase(repository)(endpoint, "t", progress::add).getOrThrow()

        assertEquals(listOf("ok.m4a"), repository.staged)
        val applied = repository.appliedPlan!!.newAudioMedias
        assertEquals(listOf(101, 104), applied.map { it.id })
        assertEquals("re.m4a", applied.first { it.id == 101 }.audioFileName)
        assertEquals(42L, applied.first { it.id == 101 }.rawAudioDurationSeconds)
        assertEquals(1, summary.redownloadedCount)
        assertEquals(
            setOf(
                MigrationFailure("곡102", MigrationFailureReason.UNSUPPORTED_FORMAT),
                MigrationFailure("곡103", MigrationFailureReason.REDOWNLOAD_FAILED),
            ),
            summary.failures.toSet(),
        )
        assertTrue(MigrationProgress(MigrationStage.REDOWNLOAD, 2, 2) in progress)
    }

    @Test
    fun `그 파일만 받을 수 없으면 그 곡만 빼고 나머지는 반영한다`() = runTest {
        val repository = FakeRepository(
            export = export(
                audios = listOf(audio(101, "s1", "a.m4a"), audio(102, "s2", "b.m4a")),
                files = listOf(entry("a.m4a"), entry("b.m4a")),
            ),
            target = emptyTarget,
            unavailableFiles = setOf("a.m4a"),
        )

        val summary = useCase(repository)(endpoint, "t") {}.getOrThrow()

        assertEquals(listOf(102), repository.appliedPlan!!.newAudioMedias.map { it.id })
        assertTrue(repository.appliedPlan!!.requiredFiles.none { it.name == "a.m4a" })
        assertEquals(listOf(MigrationFailure("곡101", MigrationFailureReason.TRANSFER_FAILED)), summary.failures)
    }

    @Test
    fun `편집본만 받을 수 없으면 곡은 원본으로 들어간다`() = runTest {
        val repository = FakeRepository(
            export = export(
                audios = listOf(audio(101, "s1", "a.m4a", edited = "a-edit.m4a")),
                files = listOf(entry("a.m4a"), entry("a-edit.m4a")),
            ),
            target = emptyTarget,
            unavailableFiles = setOf("a-edit.m4a"),
        )

        val summary = useCase(repository)(endpoint, "t") {}.getOrThrow()

        val applied = repository.appliedPlan!!.newAudioMedias.single()
        assertNull(applied.editedAudioFileName)
        assertNull(applied.editedAudioDurationSeconds)
        assertTrue(summary.failures.isEmpty())
    }

    @Test
    fun `연결이 끊기면 반영하지 않고 멈추며 다시 받은 파일을 지운다`() = runTest {
        coEvery { audioMediaRepository.downloadAudioFile(any()) } returns
            Result.success(DownloadedAudioFile("re.m4a", "/audios/re.m4a", durationSeconds = 42))
        val repository = FakeRepository(
            export = export(
                audios = listOf(audio(101, "https://youtu.be/yt", "yt.webm"), audio(102, "s2", "b.m4a")),
                files = listOf(entry("yt.webm"), entry("b.m4a")),
            ),
            target = emptyTarget,
            disconnectAt = "b.m4a",
        )

        val result = useCase(repository)(endpoint, "t") {}

        assertTrue(result.isFailure)
        assertTrue("apply" !in repository.calls)
        assertTrue("clear" !in repository.calls)
        assertTrue("finish" !in repository.calls)
        coVerify { audioMediaRepository.deleteFileAtFullPath("/audios/re.m4a") }
    }

    @Test
    fun `반영이 끝난 뒤의 정리 실패는 결과를 되돌리지 않고 다시 받은 파일도 지우지 않는다`() = runTest {
        coEvery { audioMediaRepository.downloadAudioFile(any()) } returns
            Result.success(DownloadedAudioFile("re.m4a", "/audios/re.m4a", durationSeconds = 42))
        val repository = FakeRepository(
            export = export(listOf(audio(101, "https://youtu.be/yt", "yt.webm")), listOf(entry("yt.webm"))),
            target = emptyTarget,
            failClearStaging = true,
        )

        val result = useCase(repository)(endpoint, "t") {}

        assertTrue(result.isSuccess)
        assertTrue("apply" in repository.calls)
        coVerify(exactly = 0) { audioMediaRepository.deleteFileAtFullPath(any()) }
    }

    @Test
    fun `취소는 실패로 기록하지 않고 그대로 올리며 다시 받은 파일은 지운다`() = runTest {
        coEvery { audioMediaRepository.downloadAudioFile("https://youtu.be/a") } returns
            Result.success(DownloadedAudioFile("re.m4a", "/audios/re.m4a", durationSeconds = 42))
        coEvery { audioMediaRepository.downloadAudioFile("https://youtu.be/b") } returns
            Result.failure(CancellationException("나가기"))
        val repository = FakeRepository(
            export = export(
                listOf(audio(101, "https://youtu.be/a", "a.webm"), audio(102, "https://youtu.be/b", "b.webm")),
                listOf(entry("a.webm"), entry("b.webm")),
            ),
            target = emptyTarget,
        )

        val thrown = runCatching { useCase(repository)(endpoint, "t") {} }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
        assertTrue("apply" !in repository.calls)
    }

    @Test
    fun `다운로드 설정을 옮기고 대상 플레이리스트는 받는 쪽 id 로 바꾼다`() = runTest {
        val sourcePlaylists = listOf(
            MigrationPlaylist(11, "전체", "", null, 0, PlaylistType.TOTAL, 0),
            MigrationPlaylist(12, "미분류", "", null, 0, PlaylistType.UNCATEGORIZED, 0),
            MigrationPlaylist(13, "즐겨찾기", "", null, 0, PlaylistType.FAVORITE, 0),
            MigrationPlaylist(14, "새 목록", "", null, 1, PlaylistType.USER, 0),
            MigrationPlaylist(15, "있는 목록", "", null, 2, PlaylistType.USER, 0),
        )
        val target = emptyTarget.copy(
            playlists = targetSystem + MigrationPlaylist(7, "있는 목록", "", null, 1, PlaylistType.USER, 0),
        )
        suspend fun runWith(settings: MigrationDownloadSettings) = useCase(
            FakeRepository(export(emptyList(), emptyList(), sourcePlaylists, settings), target),
        )(endpoint, "t") {}.getOrThrow()

        runWith(MigrationDownloadSettings(shouldInsertAtTop = true, playlistId = 14))
        coVerify { appSettingRepository.updateShouldInsertAtTopOnDownload(true) }
        coVerify { appSettingRepository.updatePlaylistIdOnDownload(1014) }

        runWith(MigrationDownloadSettings(shouldInsertAtTop = false, playlistId = 15))
        coVerify { appSettingRepository.updatePlaylistIdOnDownload(7) }

        runWith(MigrationDownloadSettings(shouldInsertAtTop = false, playlistId = null))
        coVerify { appSettingRepository.updatePlaylistIdOnDownload(2) }
    }

    @Test
    fun `설정이 없는 manifest 면 받는 기기 설정을 건드리지 않는다`() = runTest {
        useCase(FakeRepository(export(emptyList(), emptyList()), emptyTarget))(endpoint, "t") {}.getOrThrow()

        coVerify(exactly = 0) { appSettingRepository.updateShouldInsertAtTopOnDownload(any()) }
        coVerify(exactly = 0) { appSettingRepository.updatePlaylistIdOnDownload(any()) }
    }
}
