package com.kintmin.data.repository_impl

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_file.DirectoryFileManager
import com.kintmin.data.network.dataSource.MigrationDataSource
import com.kintmin.domain.data_migration.MigrationPlanner
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationFileUnavailableException
import com.kintmin.domain.data_migration.model.MigrationRef
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationTrackAddition
import com.kintmin.fileshare.MigrationManifestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataMigrationRepositoryImplTest {

    private val fileSystem = FileSystem.SYSTEM
    private val root: Path = (NSTemporaryDirectory() + NSUUID().UUIDString).toPath()
    private val endpoint = MigrationEndpoint("host", 1)
    private val remoteFiles = mutableMapOf("a.m4a" to byteArrayOf(1, 2, 3, 4))
    private var downloadCount = 0

    private lateinit var db: JellyTubeDatabase
    private lateinit var facade: AudioMediaFacade
    private lateinit var repository: DataMigrationRepositoryImpl

    @BeforeTest
    fun setup() {
        fileSystem.createDirectories(root)
        db = Room.inMemoryDatabaseBuilder<JellyTubeDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        facade = AudioMediaFacade(
            db = db,
            audioMediaDao = db.audioMediaDao(),
            playlistDao = db.playlistDao(),
            playlistTrackDao = db.playlistTrackDao(),
            audioEditProjectDao = db.audioEditProjectDao(),
            stepDao = db.stepDao(),
        )
        repository = DataMigrationRepositoryImpl(facade, DirectoryFileManager(root), FakeDataSource())
    }

    @AfterTest
    fun tearDown() {
        db.close()
        fileSystem.deleteRecursively(root)
    }

    private val audioEntry = MigrationFileEntry(MigrationFile(MigrationFileKind.AUDIO, "a.m4a"), size = 4)

    @Test
    fun `받아 둔 파일은 다시 받지 않는다`() = runTest {
        repository.stageFile(endpoint, "t", audioEntry) {}.getOrThrow()
        repository.stageFile(endpoint, "t", audioEntry) {}.getOrThrow()

        assertEquals(1, downloadCount)
        assertTrue(fileSystem.exists(root / "staging" / "AUDIO" / "a.m4a"))
    }

    @Test
    fun `받은 크기가 다르면 실패하고 받아 둔 파일로 남지 않는다`() = runTest {
        val result = repository.stageFile(endpoint, "t", audioEntry.copy(size = 10)) {}

        // 그 파일 하나의 문제다. 이전 전체를 멈추지 않도록 따로 구분한다.
        assertTrue(result.exceptionOrNull() is MigrationFileUnavailableException)
        assertFalse(fileSystem.exists(root / "staging" / "AUDIO" / "a.m4a"))
    }

    @Test
    fun `반영하면 받아 둔 파일이 제자리로 가고 임시 공간을 비울 수 있다`() = runTest {
        facade.ensureSystemPlaylists()
        repository.stageFile(endpoint, "t", audioEntry) {}.getOrThrow()
        val plan = MigrationPlanner.plan(sourceWithOneAudio(), facade.fetchMigrationSnapshot())

        repository.applyPlan(plan).getOrThrow()
        repository.clearStaging().getOrThrow()

        assertTrue(byteArrayOf(1, 2, 3, 4).contentEquals(fileSystem.read(root / "audios" / "a.m4a") { readByteArray() }))
        assertFalse(fileSystem.exists(root / "staging"))
        assertEquals(1, db.audioMediaDao().getAll().size)
    }

    @Test
    fun `DB 반영이 실패하면 옮긴 파일을 지우고 아무 행도 남기지 않는다`() = runTest {
        facade.ensureSystemPlaylists()
        repository.stageFile(endpoint, "t", audioEntry) {}.getOrThrow()
        val plan = MigrationPlanner.plan(sourceWithOneAudio(), facade.fetchMigrationSnapshot()).let {
            it.copy(trackAdditions = it.trackAdditions + MigrationTrackAddition(MigrationRef.Existing(9999), listOf(MigrationRef.Incoming(101))))
        }

        val result = repository.applyPlan(plan)

        assertTrue(result.isFailure)
        assertFalse(fileSystem.exists(root / "audios" / "a.m4a"))
        assertTrue(db.audioMediaDao().getAll().isEmpty())
    }

    @Test
    fun `옮기다 복사가 실패하면 반쯤 쓴 파일을 남기지 않는다`() {
        val source = root / "staged.m4a"
        val destination = root / "a.m4a"
        fileSystem.write(source) { write(byteArrayOf(1, 2, 3, 4)) }

        assertFails { FailingCopyFileSystem(fileSystem).moveFile(source, destination) }

        assertFalse(fileSystem.exists(destination))
        assertTrue(fileSystem.exists(source))
    }

    /** 다른 볼륨이라 바로 옮기지 못하고, 복사는 한 바이트 쓰고 실패한다. */
    private class FailingCopyFileSystem(delegate: FileSystem) : ForwardingFileSystem(delegate) {
        override fun atomicMove(source: Path, target: Path) = throw IOException("다른 볼륨")

        override fun sink(file: Path, mustCreate: Boolean): Sink {
            val delegate = super.sink(file, mustCreate)
            return object : Sink by delegate {
                override fun write(source: Buffer, byteCount: Long) {
                    delegate.write(source, 1)
                    throw IOException("저장 공간 부족")
                }
            }
        }
    }

    private fun sourceWithOneAudio() = MigrationSnapshot(
        audioMedias = listOf(
            MigrationAudioMedia(
                id = 101, source = "s", name = "곡", artist = "", description = "", rawAudioDurationSeconds = 1,
                audioFileName = "a.m4a", imageFileName = null, lyricFileName = null, tjKaraokeNumber = null,
                editedAudioFileName = null, editedAudioDurationSeconds = null, createdTimeMillis = 0,
            ),
        ),
        playlists = emptyList(), tracks = emptyList(), editProjects = emptyList(), steps = emptyList(),
    )

    private inner class FakeDataSource : MigrationDataSource {
        override suspend fun pair(host: String, port: Int, code: String) = Result.success("t")
        override suspend fun fetchManifest(host: String, port: Int, token: String) =
            Result.failure<MigrationManifestDto>(UnsupportedOperationException())

        override suspend fun finish(host: String, port: Int, token: String, newAudioMediaCount: Int, failedCount: Int) =
            Result.success(Unit)

        override suspend fun downloadFile(
            host: String,
            port: Int,
            token: String,
            kind: String,
            name: String,
            onChunk: (ByteArray, Int) -> Unit,
        ): Result<Unit> {
            downloadCount++
            val bytes = remoteFiles.getValue(name)
            onChunk(bytes, bytes.size)
            return Result.success(Unit)
        }
    }
}
