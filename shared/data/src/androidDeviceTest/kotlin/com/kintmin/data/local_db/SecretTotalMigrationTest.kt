package com.kintmin.data.local_db

import androidx.room.Room
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.database.MIGRATION_8_9_STATEMENTS
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.domain.playlist.model.PlaylistType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v8 → v9 는 스키마를 바꾸지 않고 데이터만 옮긴다. 그래서 현재 스키마의 DB 에 **v8 모양**을
 * 손으로 만들어 두고 같은 SQL 을 돌려 결과를 본다.
 */
@RunWith(AndroidJUnit4::class)
class SecretTotalMigrationTest {

    private lateinit var db: JellyTubeDatabase
    private lateinit var facade: AudioMediaFacade

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder<JellyTubeDatabase>(
            context = ApplicationProvider.getApplicationContext()
        )
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
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun exec(sql: String) {
        db.useWriterConnection { transactor ->
            transactor.usePrepared(sql) { statement -> statement.step() }
        }
    }

    private suspend fun addAudioMedia(name: String, durationSeconds: Long): Int =
        facade.addNewAudioMedia(
            newAudioMedia = AudioMediaEntity(
                source = "https://example.com/$name",
                name = name,
                artist = "아티스트",
                description = "",
                rawAudioDurationSeconds = durationSeconds,
                audioFileNameWithExt = "$name.m4a",
                imageFileNameWithExt = "$name.webp",
            ),
            requestedPlaylistId = null,
            shouldInsertAtTopOnDownload = false,
        ).audioMediaId

    private suspend fun idOf(type: PlaylistType): Int? = db.playlistDao().getPlaylistIdByType(type.name)

    private suspend fun trackIds(playlistId: Int): List<Int> =
        db.playlistTrackDao().getPlaylistTrackFullListFlow(playlistId).first().map { it.audioMediaEntity.id }

    @Test
    fun 전체에_남아_있던_비밀_곡을_비밀_전체로_옮기고_두_집계를_다시_센다() = runTest {
        facade.ensureSystemPlaylists()
        // v8 에는 "비밀 전체" 가 없었고, 비밀 곡도 "전체" 에 담긴 채였다.
        exec("DELETE FROM PLAYLIST WHERE type = '${PlaylistType.SECRET_TOTAL.name}'")
        val visible = addAudioMedia("visible", 100)
        val secret = addAudioMedia("secret", 40)
        exec(
            """
INSERT INTO PLAYLIST (name, description, audioMediaCount, rawPlayTimeDuration, rawCreatedTime, imageFileNameWithExt, isCustomImage, sequence, type)
VALUES ('비밀', '', 1, 40, 0, 'secret.webp', 0, 0, '${PlaylistType.SECRET.name}')
""",
        )
        val secretPlaylistId = idOf(PlaylistType.SECRET)!!
        exec(
            "INSERT INTO PLAYLIST_TRACK (playlistId, audioMediaId, sequence, rawCreatedTime) " +
                "VALUES ($secretPlaylistId, $secret, 1, 0)",
        )
        val totalId = idOf(PlaylistType.TOTAL)!!
        assertEquals(listOf(visible, secret), trackIds(totalId))

        MIGRATION_8_9_STATEMENTS.forEach { exec(it) }

        val secretTotalId = idOf(PlaylistType.SECRET_TOTAL)!!
        assertEquals(listOf(visible), trackIds(totalId))
        assertEquals(listOf(secret), trackIds(secretTotalId))

        val total = db.playlistDao().getPlaylistById(totalId)
        assertEquals(1, total.audioMediaCount)
        assertEquals(100L, total.rawPlayTimeDuration)
        assertEquals("visible.webp", total.imageFileNameWithExt)

        val secretTotal = db.playlistDao().getPlaylistById(secretTotalId)
        assertEquals(PlaylistType.SECRET_TOTAL.defaultName, secretTotal.name)
        assertEquals(1, secretTotal.audioMediaCount)
        assertEquals(40L, secretTotal.rawPlayTimeDuration)
        assertEquals("secret.webp", secretTotal.imageFileNameWithExt)
    }

    @Test
    fun 비밀을_쓴_적이_없으면_빈_비밀_전체만_생기고_전체는_그대로다() = runTest {
        facade.ensureSystemPlaylists()
        exec("DELETE FROM PLAYLIST WHERE type = '${PlaylistType.SECRET_TOTAL.name}'")
        val visible = addAudioMedia("visible", 100)
        val totalId = idOf(PlaylistType.TOTAL)!!

        MIGRATION_8_9_STATEMENTS.forEach { exec(it) }

        assertEquals(listOf(visible), trackIds(totalId))
        val secretTotal = db.playlistDao().getPlaylistById(idOf(PlaylistType.SECRET_TOTAL)!!)
        assertEquals(0, secretTotal.audioMediaCount)
        assertEquals(0L, secretTotal.rawPlayTimeDuration)
        assertEquals(null, secretTotal.imageFileNameWithExt)
    }

    @Test
    fun 두_번_돌려도_비밀_전체가_하나다() = runTest {
        facade.ensureSystemPlaylists()
        exec("DELETE FROM PLAYLIST WHERE type = '${PlaylistType.SECRET_TOTAL.name}'")

        MIGRATION_8_9_STATEMENTS.forEach { exec(it) }
        MIGRATION_8_9_STATEMENTS.forEach { exec(it) }

        val count = db.playlistDao().getAll().count { it.type == PlaylistType.SECRET_TOTAL.name }
        assertEquals(1, count)
    }
}
