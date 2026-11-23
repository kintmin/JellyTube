package com.kintmin.domain.backup

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.audio_media.model.DownloadedAudioFile
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.usecase.RestoreBackupUseCase
import com.kintmin.domain.data_migration.model.MigrationApplyResult
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationFailure
import com.kintmin.domain.data_migration.model.MigrationFailureReason
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationProgress
import com.kintmin.domain.data_migration.model.MigrationRef
import com.kintmin.domain.data_migration.model.MigrationScope
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationStage
import com.kintmin.domain.data_migration.model.MigrationStep
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.domain.data_migration.model.MigrationTrack
import com.kintmin.domain.data_migration.model.MigrationTrackAddition
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreBackupUseCaseTest {

    private val playlistRepository = mockk<PlaylistRepository> {
        coEvery { ensureSystemPlaylists() } returns Result.success(Unit)
    }
    private val appSettingRepository = mockk<AppSettingRepository> {
        coEvery { updateShouldInsertAtTopOnDownload(any()) } returns Result.success(Unit)
        coEvery { updatePlaylistIdOnDownload(any()) } returns Result.success(Unit)
    }
    private val audioMediaRepository = mockk<AudioMediaRepository> {
        coEvery { downloadAudioFile(any()) } answers {
            val id = firstArg<String>().substringAfterLast('/')
            Result.success(DownloadedAudioFile("uuid-$id.m4a", "/audio/uuid-$id.m4a", 200))
        }
        coEvery { saveThumbnail(any(), any()) } answers { Result.success("${secondArg<String>()}.webp") }
        coEvery { deleteFileAtFullPath(any()) } returns Result.success(Unit)
    }
    private val appLog = mockk<AppLog>(relaxed = true)
    private val targetSystem = listOf(
        MigrationPlaylist(1, "전체", "", null, 0, PlaylistType.TOTAL, 0),
        MigrationPlaylist(2, "미분류", "", null, 0, PlaylistType.UNCATEGORIZED, 0),
        MigrationPlaylist(3, "즐겨찾기", "", null, 0, PlaylistType.FAVORITE, 0),
    )
    private val emptyTarget = MigrationSnapshot(emptyList(), targetSystem, emptyList(), emptyList(), emptyList())

    private fun audio(
        id: Int,
        source: String,
        audioFile: String,
        image: String? = null,
        lyric: String? = null,
        edited: String? = null,
    ) = MigrationAudioMedia(
        id = id, source = source, name = "곡$id", artist = "", description = "", rawAudioDurationSeconds = 1,
        audioFileName = audioFile, imageFileName = image, lyricFileName = lyric, tjKaraokeNumber = null,
        editedAudioFileName = edited, editedAudioDurationSeconds = edited?.let { 1 }, createdTimeMillis = id.toLong(),
    )

    private fun backup(
        audios: List<MigrationAudioMedia>,
        playlists: List<MigrationPlaylist> = emptyList(),
        tracks: List<MigrationTrack> = emptyList(),
        steps: List<MigrationStep> = emptyList(),
        lyrics: Map<String, String> = emptyMap(),
    ) = BackupContent(MigrationSnapshot(audios, playlists, tracks, emptyList(), steps), null, lyrics, 0)

    private class FakeRepository(
        private val target: MigrationSnapshot,
        private val alreadyStaged: Set<MigrationFile> = emptySet(),
        private val failApply: Boolean = false,
        /** 반영이 커밋된 직후, 돌아오기 전에 부른다. */
        private val afterApplyCommitted: () -> Unit = {},
    ) : DataMigrationRepository {
        val calls = mutableListOf<String>()
        val staged = mutableSetOf<MigrationFile>()
        val stagedTexts = mutableMapOf<String, String>()
        var appliedPlan: MigrationPlan? = null

        override suspend fun exportData(scope: MigrationScope) = error("복원에서 부르지 않는다")
        override fun resolveFilePath(file: MigrationFile) = Result.success("/${file.kind}/${file.name}")
        override suspend fun fetchSnapshot() = Result.success(target)
        override fun isPlayableAudioFile(fileName: String) = error("복원에서 부르지 않는다")
        override suspend fun pair(endpoint: MigrationEndpoint, code: String) = error("복원에서 부르지 않는다")
        override suspend fun fetchRemoteExport(endpoint: MigrationEndpoint, token: String) = error("복원에서 부르지 않는다")
        override suspend fun stageFile(
            endpoint: MigrationEndpoint,
            token: String,
            entry: MigrationFileEntry,
            onReceived: (Long) -> Unit,
        ) = error("복원에서 부르지 않는다")

        override suspend fun stageLocalFile(fileFullPath: String, file: MigrationFile): Result<Unit> {
            staged += file
            return Result.success(Unit)
        }

        override suspend fun stageText(file: MigrationFile, text: String): Result<Unit> {
            staged += file
            stagedTexts[file.name] = text
            return Result.success(Unit)
        }

        override suspend fun fetchStagedFiles() = Result.success(alreadyStaged)

        override suspend fun applyPlan(plan: MigrationPlan): Result<MigrationApplyResult> {
            calls += "apply"
            if (failApply) return Result.failure(IllegalStateException("디스크 가득"))
            appliedPlan = plan
            afterApplyCommitted()
            return Result.success(MigrationApplyResult(plan.newPlaylists.associate { it.id to it.id + 1000 }))
        }

        override suspend fun clearStaging(): Result<Unit> {
            calls += "clear"
            return Result.success(Unit)
        }

        override suspend fun notifyFinished(endpoint: MigrationEndpoint, token: String, summary: MigrationSummary) =
            error("복원에서 부르지 않는다")

        override suspend fun notifyDeclined(endpoint: MigrationEndpoint, token: String) =
            error("복원에서 부르지 않는다")
    }

    private fun useCase(repository: DataMigrationRepository) =
        RestoreBackupUseCase(repository, playlistRepository, appSettingRepository, audioMediaRepository, appLog)

    @Test
    fun `유튜브 곡은 다시 받아 백업의 이름으로 쌓고 한 번에 반영한다`() = runTest {
        val repository = FakeRepository(emptyTarget)
        val progress = mutableListOf<MigrationProgress>()

        val summary = useCase(repository)(
            backup(
                audios = listOf(audio(101, "https://youtu.be/a", "aaa.webm", image = "img1.jpg", lyric = "l1.lrc")),
                lyrics = mapOf("l1.lrc" to "[00:01.00]가사"),
            ),
            progress::add,
        ).getOrThrow()

        val restored = repository.appliedPlan!!.newAudioMedias.single()
        assertEquals("aaa.m4a", restored.audioFileName)
        assertEquals("img1.webp", restored.imageFileName)
        assertEquals("l1.lrc", restored.lyricFileName)
        assertEquals(200L, restored.rawAudioDurationSeconds)
        assertEquals(
            setOf(
                MigrationFile(MigrationFileKind.AUDIO, "aaa.m4a"),
                MigrationFile(MigrationFileKind.IMAGE, "img1.webp"),
                MigrationFile(MigrationFileKind.LYRIC, "l1.lrc"),
            ),
            repository.staged,
        )
        assertEquals(repository.staged, repository.appliedPlan!!.requiredFiles)
        assertEquals("[00:01.00]가사", repository.stagedTexts["l1.lrc"])
        coVerify { audioMediaRepository.saveThumbnail("https://i.ytimg.com/vi/a/hqdefault.jpg", "img1") }
        assertEquals(listOf("apply", "clear"), repository.calls)
        assertEquals(MigrationSummary(1, 0, 1, emptyList()), summary)
        assertEquals(
            listOf(MigrationStage.PLANNING, MigrationStage.REDOWNLOAD, MigrationStage.REDOWNLOAD, MigrationStage.APPLY),
            progress.map { it.stage },
        )
    }

    @Test
    fun `원본이 유튜브가 아닌 곡은 받지 않고 복원 불가로 남긴다`() = runTest {
        val repository = FakeRepository(emptyTarget)

        val summary = useCase(repository)(backup(listOf(audio(101, "content://media/1", "a.m4a"))), {}).getOrThrow()

        assertEquals(listOf(MigrationFailure("곡101", MigrationFailureReason.NOT_RESTORABLE)), summary.failures)
        assertTrue(repository.appliedPlan!!.newAudioMedias.isEmpty())
        coVerify(exactly = 0) { audioMediaRepository.downloadAudioFile(any()) }
    }

    @Test
    fun `이미 있는 곡은 받지 않고 기존 곡을 재생목록에 넣는다`() = runTest {
        val target = emptyTarget.copy(audioMedias = listOf(audio(7, "https://www.youtube.com/watch?v=a", "mine.m4a")))
        val repository = FakeRepository(target)

        useCase(repository)(
            backup(
                audios = listOf(audio(101, "https://youtu.be/a", "aaa.m4a")),
                playlists = listOf(MigrationPlaylist(50, "드라이브", "", null, 0, PlaylistType.USER, 0)),
                tracks = listOf(MigrationTrack(50, 101, 0)),
            ),
            {},
        ).getOrThrow()

        val plan = repository.appliedPlan!!
        assertTrue(plan.newAudioMedias.isEmpty())
        assertTrue(MigrationTrackAddition(MigrationRef.Incoming(50), listOf(MigrationRef.Existing(7))) in plan.trackAdditions)
        coVerify(exactly = 0) { audioMediaRepository.downloadAudioFile(any()) }
    }

    @Test
    fun `임시 공간에 쌓여 있는 곡은 다시 받지 않는다`() = runTest {
        val repository = FakeRepository(emptyTarget, alreadyStaged = setOf(MigrationFile(MigrationFileKind.AUDIO, "aaa.m4a")))

        useCase(repository)(backup(listOf(audio(101, "https://youtu.be/a", "aaa.webm"))), {}).getOrThrow()

        assertEquals("aaa.m4a", repository.appliedPlan!!.newAudioMedias.single().audioFileName)
        coVerify(exactly = 0) { audioMediaRepository.downloadAudioFile(any()) }
    }

    @Test
    fun `다시 받지 못한 곡만 빼고 나머지는 반영한다`() = runTest {
        coEvery { audioMediaRepository.downloadAudioFile("https://youtu.be/b") } returns Result.failure(IllegalStateException("429"))
        val repository = FakeRepository(emptyTarget)

        val summary = useCase(repository)(
            backup(listOf(audio(101, "https://youtu.be/a", "a.m4a"), audio(102, "https://youtu.be/b", "b.m4a"))),
            {},
        ).getOrThrow()

        assertEquals(listOf(101), repository.appliedPlan!!.newAudioMedias.map { it.id })
        assertEquals(listOf(MigrationFailure("곡102", MigrationFailureReason.REDOWNLOAD_FAILED)), summary.failures)
        coVerify(exactly = 2) { audioMediaRepository.downloadAudioFile("https://youtu.be/b") }
    }

    @Test
    fun `썸네일을 받지 못해도 곡은 들이고 이미지만 비운다`() = runTest {
        coEvery { audioMediaRepository.saveThumbnail(any(), any()) } returns Result.failure(IllegalStateException("404"))
        val repository = FakeRepository(emptyTarget)

        useCase(repository)(backup(listOf(audio(101, "https://youtu.be/a", "a.m4a", image = "i.jpg"))), {}).getOrThrow()

        assertNull(repository.appliedPlan!!.newAudioMedias.single().imageFileName)
    }

    @Test
    fun `편집본과 커스텀 플레이리스트 이미지는 복원하지 않는다`() = runTest {
        val repository = FakeRepository(emptyTarget)

        useCase(repository)(
            backup(
                audios = listOf(audio(101, "https://youtu.be/a", "a.m4a", edited = "a-edit.m4a")),
                playlists = listOf(MigrationPlaylist(50, "드라이브", "", "cover.webp", 0, PlaylistType.USER, 0)),
            ),
            {},
        ).getOrThrow()

        val plan = repository.appliedPlan!!
        assertNull(plan.newAudioMedias.single().editedAudioFileName)
        assertNull(plan.newPlaylists.single().customImageFileName)
        assertEquals(setOf(MigrationFile(MigrationFileKind.AUDIO, "a.m4a")), plan.requiredFiles)
    }

    @Test
    fun `걸음수는 그대로 넘긴다`() = runTest {
        val repository = FakeRepository(emptyTarget)
        val steps = listOf(MigrationStep(1_000, 42))

        useCase(repository)(backup(audios = emptyList(), steps = steps), {}).getOrThrow()

        assertEquals(steps, repository.appliedPlan!!.steps)
    }

    @Test
    fun `반영이 커밋된 직후 멈춰도 임시 공간 정리와 다운로드 설정 반영까지 마친다`() = runTest {
        lateinit var job: Job
        val repository = FakeRepository(emptyTarget, afterApplyCommitted = { job.cancel() })
        val withSettings = backup(listOf(audio(101, "https://youtu.be/a", "a.m4a")))
            .copy(downloadSettings = MigrationDownloadSettings(shouldInsertAtTop = true, playlistId = null))

        job = launch { useCase(repository)(withSettings) {} }
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(listOf("apply", "clear"), repository.calls)
        coVerify { appSettingRepository.updateShouldInsertAtTopOnDownload(true) }
    }

    @Test
    fun `반영이 실패하면 쌓아 둔 파일을 비우지 않아 다시 부르면 이어 받는다`() = runTest {
        val repository = FakeRepository(emptyTarget, failApply = true)

        val result = useCase(repository)(backup(listOf(audio(101, "https://youtu.be/a", "a.m4a"))), {})

        assertTrue(result.isFailure)
        assertEquals(listOf("apply"), repository.calls)
    }
}
