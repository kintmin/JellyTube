package com.kintmin.data.local_file

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.model.AudioMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioFileExtensionCorrectorTest {

    private val fileSystem = FileSystem.SYSTEM
    private val root: Path = (NSTemporaryDirectory() + NSUUID().UUIDString).toPath()
    private lateinit var db: JellyTubeDatabase
    private lateinit var corrector: AudioFileExtensionCorrector

    // YouTube 140(m4a)·251(webm) 의 실제 앞부분.
    private val m4aHeader = "0000001866747970646173680000000069736f366d703431".hexToBytes()
    private val webmHeader = "1a45dfa39f4286810142f7810142f2810442f38108428284".hexToBytes()

    @BeforeTest
    fun setup() {
        fileSystem.createDirectories(root)
        db = Room.inMemoryDatabaseBuilder<JellyTubeDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        corrector = AudioFileExtensionCorrector(db.audioMediaDao(), DirectoryFileManager(root))
    }

    @AfterTest
    fun tearDown() {
        db.close()
        fileSystem.deleteRecursively(root)
    }

    private fun writeAudio(name: String, header: ByteArray) {
        fileSystem.createDirectories(root / "audios")
        fileSystem.write(root / "audios" / name) { write(header) }
    }

    private suspend fun insert(audio: String, edited: String? = null): Int = db.audioMediaDao().insertAudioMedia(
        AudioMediaEntity(
            source = "s$audio",
            name = "곡",
            artist = "",
            description = "",
            audioFileNameWithExt = audio,
            editedAudioFileNameWithExt = edited,
            editedAudioDurationSeconds = edited?.let { 1 },
        )
    ).toInt()

    @Test
    fun `내용과 다른 확장자는 파일과 DB 를 함께 바로잡는다`() = runTest {
        writeAudio("a.mp3", m4aHeader)
        writeAudio("e.m4a", webmHeader)
        val id = insert("a.mp3", edited = "e.m4a")

        assertEquals(2, corrector.correctAll())

        val entity = db.audioMediaDao().getDataById(id)
        assertEquals("a.m4a", entity.audioFileNameWithExt)
        assertEquals("e.webm", entity.editedAudioFileNameWithExt)
        assertTrue(fileSystem.exists(root / "audios" / "a.m4a"))
        assertTrue(!fileSystem.exists(root / "audios" / "a.mp3"))
    }

    @Test
    fun `맞는 확장자와 없는 파일은 건드리지 않고 다시 돌려도 바뀌지 않는다`() = runTest {
        writeAudio("ok.m4a", m4aHeader)
        insert("ok.m4a")
        insert("missing.mp3")

        assertEquals(0, corrector.correctAll())
        assertEquals(listOf("ok.m4a", "missing.mp3"), db.audioMediaDao().getAll().map { it.audioFileNameWithExt })
    }

    private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
