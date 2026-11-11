package com.kintmin.domain.data_migration

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.data_migration.model.MigrationApplyResult
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationProgress
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.domain.data_migration.usecase.ImportMigrationDataUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    private val targetSystem = listOf(
        MigrationPlaylist(1, "전체", "", null, 0, PlaylistType.TOTAL, 0),
        MigrationPlaylist(2, "미분류", "", null, 0, PlaylistType.UNCATEGORIZED, 0),
        MigrationPlaylist(3, "즐겨찾기", "", null, 0, PlaylistType.FAVORITE, 0),
    )

    private fun audio(id: Int, source: String, audioFile: String) = MigrationAudioMedia(
        id = id, source = source, name = "곡$id", artist = "", description = "", rawAudioDurationSeconds = 1,
        audioFileName = audioFile, imageFileName = null, lyricFileName = null, tjKaraokeNumber = null,
        editedAudioFileName = null, editedAudioDurationSeconds = null, createdTimeMillis = id.toLong(),
    )

    private fun entry(name: String, size: Long) = MigrationFileEntry(MigrationFile(MigrationFileKind.AUDIO, name), size)

    private class FakeRepository(
        private val export: MigrationExport,
        private val target: MigrationSnapshot,
        private val failStaging: Boolean = false,
    ) : DataMigrationRepository {
        val calls = mutableListOf<String>()
        val staged = mutableListOf<String>()
        var appliedPlan: MigrationPlan? = null

        override suspend fun exportData() = error("받는 쪽에서 부르지 않는다")
        override fun resolveFilePath(file: MigrationFile) = error("받는 쪽에서 부르지 않는다")
        override suspend fun fetchSnapshot() = Result.success(target).also { calls += "fetchSnapshot" }
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
            if (failStaging) return Result.failure(IllegalStateException("끊김"))
            staged += entry.file.name
            onReceived(entry.size / 2)
            return Result.success(Unit)
        }

        override suspend fun applyPlan(plan: MigrationPlan) =
            Result.success(MigrationApplyResult(plan.newPlaylists.associate { it.id to it.id + 1000 })).also {
                calls += "apply"
                appliedPlan = plan
            }

        override suspend fun clearStaging() = Result.success(Unit).also { calls += "clear" }
    }

    @Test
    fun `새 곡의 파일만 받아 둔 뒤 반영하고 임시 공간을 비운다`() = runTest {
        val repository = FakeRepository(
            export = MigrationExport(
                snapshot = MigrationSnapshot(
                    audioMedias = listOf(audio(101, "https://youtu.be/new", "new.m4a"), audio(102, "https://youtu.be/old", "old.m4a")),
                    playlists = emptyList(), tracks = emptyList(), editProjects = emptyList(), steps = emptyList(),
                ),
                files = listOf(entry("new.m4a", 100), entry("old.m4a", 50)),
                downloadSettings = null,
            ),
            target = MigrationSnapshot(
                audioMedias = listOf(audio(7, "https://www.youtube.com/watch?v=old", "mine.m4a")),
                playlists = targetSystem, tracks = emptyList(), editProjects = emptyList(), steps = emptyList(),
            ),
        )
        val progress = mutableListOf<MigrationProgress>()

        val summary = ImportMigrationDataUseCase(repository, playlistRepository, appSettingRepository)(endpoint, "t", progress::add).getOrThrow()

        assertEquals(listOf("new.m4a"), repository.staged)
        assertEquals(listOf("fetchRemoteExport", "fetchSnapshot", "stage", "apply", "clear"), repository.calls)
        assertEquals(listOf(101), repository.appliedPlan?.newAudioMedias?.map { it.id })
        assertEquals(1, summary.newAudioMediaCount)
        assertEquals(MigrationProgress(0, 100), progress.first())
        assertTrue(MigrationProgress(50, 100) in progress)
        assertEquals(MigrationProgress(100, 100), progress.last())
        coVerify(exactly = 1) { playlistRepository.ensureSystemPlaylists() }
    }

    @Test
    fun `위험한 이름의 파일은 받지 않고 그 파일이 음원인 곡은 들여오지 않는다`() = runTest {
        val repository = FakeRepository(
            export = MigrationExport(
                snapshot = MigrationSnapshot(
                    audioMedias = listOf(audio(101, "s", "../evil.m4a")),
                    playlists = emptyList(), tracks = emptyList(), editProjects = emptyList(), steps = emptyList(),
                ),
                files = listOf(entry("../evil.m4a", 10)),
                downloadSettings = null,
            ),
            target = MigrationSnapshot(emptyList(), targetSystem, emptyList(), emptyList(), emptyList()),
        )

        ImportMigrationDataUseCase(repository, playlistRepository, appSettingRepository)(endpoint, "t") {}.getOrThrow()

        assertTrue(repository.staged.isEmpty())
        assertTrue(repository.appliedPlan?.newAudioMedias.orEmpty().isEmpty())
    }

    @Test
    fun `다운로드 설정을 옮기고 대상 플레이리스트는 받는 쪽 id 로 바꾼다`() = runTest {
        val sourceSystem = listOf(
            MigrationPlaylist(11, "전체", "", null, 0, PlaylistType.TOTAL, 0),
            MigrationPlaylist(12, "미분류", "", null, 0, PlaylistType.UNCATEGORIZED, 0),
            MigrationPlaylist(13, "즐겨찾기", "", null, 0, PlaylistType.FAVORITE, 0),
        )
        fun repositoryWith(settings: MigrationDownloadSettings) = FakeRepository(
            export = MigrationExport(
                snapshot = MigrationSnapshot(
                    audioMedias = emptyList(),
                    playlists = sourceSystem +
                        MigrationPlaylist(14, "새 목록", "", null, 1, PlaylistType.USER, 0) +
                        MigrationPlaylist(15, "있는 목록", "", null, 2, PlaylistType.USER, 0),
                    tracks = emptyList(), editProjects = emptyList(), steps = emptyList(),
                ),
                files = emptyList(),
                downloadSettings = settings,
            ),
            target = MigrationSnapshot(
                audioMedias = emptyList(),
                playlists = targetSystem + MigrationPlaylist(7, "있는 목록", "", null, 1, PlaylistType.USER, 0),
                tracks = emptyList(), editProjects = emptyList(), steps = emptyList(),
            ),
        )
        val useCase = { settings: MigrationDownloadSettings ->
            ImportMigrationDataUseCase(repositoryWith(settings), playlistRepository, appSettingRepository)
        }

        useCase(MigrationDownloadSettings(shouldInsertAtTop = true, playlistId = 14))(endpoint, "t") {}.getOrThrow()
        coVerify { appSettingRepository.updateShouldInsertAtTopOnDownload(true) }
        coVerify { appSettingRepository.updatePlaylistIdOnDownload(1014) }

        useCase(MigrationDownloadSettings(shouldInsertAtTop = false, playlistId = 15))(endpoint, "t") {}.getOrThrow()
        coVerify { appSettingRepository.updatePlaylistIdOnDownload(7) }

        useCase(MigrationDownloadSettings(shouldInsertAtTop = false, playlistId = null))(endpoint, "t") {}.getOrThrow()
        coVerify { appSettingRepository.updatePlaylistIdOnDownload(2) }
    }

    @Test
    fun `설정이 없는 manifest 면 받는 기기 설정을 건드리지 않는다`() = runTest {
        val repository = FakeRepository(
            export = MigrationExport(
                snapshot = MigrationSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                files = emptyList(),
                downloadSettings = null,
            ),
            target = MigrationSnapshot(emptyList(), targetSystem, emptyList(), emptyList(), emptyList()),
        )

        ImportMigrationDataUseCase(repository, playlistRepository, appSettingRepository)(endpoint, "t") {}.getOrThrow()

        coVerify(exactly = 0) { appSettingRepository.updateShouldInsertAtTopOnDownload(any()) }
        coVerify(exactly = 0) { appSettingRepository.updatePlaylistIdOnDownload(any()) }
    }

    @Test
    fun `파일 받기가 실패하면 반영하지 않는다`() = runTest {
        val repository = FakeRepository(
            export = MigrationExport(
                snapshot = MigrationSnapshot(
                    audioMedias = listOf(audio(101, "s", "a.m4a")),
                    playlists = emptyList(), tracks = emptyList(), editProjects = emptyList(), steps = emptyList(),
                ),
                files = listOf(entry("a.m4a", 10)),
                downloadSettings = null,
            ),
            target = MigrationSnapshot(emptyList(), targetSystem, emptyList(), emptyList(), emptyList()),
            failStaging = true,
        )

        val result = ImportMigrationDataUseCase(repository, playlistRepository, appSettingRepository)(endpoint, "t") {}

        assertTrue(result.isFailure)
        assertTrue("apply" !in repository.calls)
        assertTrue("clear" !in repository.calls)
    }
}
