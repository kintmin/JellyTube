package com.kintmin.data.local_db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.database.JellyTubeDatabase
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

/** 실기기/에뮬레이터에서 실행한다(Room + BundledSQLiteDriver). */
@RunWith(AndroidJUnit4::class)
class SecretPlaylistQueryTest {

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

    private suspend fun addAudioMedia(name: String, durationSeconds: Long): Int =
        facade.addNewAudioMedia(
            newAudioMedia = AudioMediaEntity(
                source = "https://example.com/$name",
                name = name,
                artist = "아티스트",
                description = "",
                rawAudioDurationSeconds = durationSeconds,
                audioFileNameWithExt = "$name.m4a",
            ),
            requestedPlaylistId = null,
            shouldInsertAtTopOnDownload = false,
        ).audioMediaId

    private suspend fun totalId(): Int = db.playlistDao().getPlaylistIdByType(PlaylistType.TOTAL.name)!!

    @Test
    fun 전체의_곡_목록과_곡_수_길이에서_비밀_곡을_뺀다() = runTest {
        facade.ensureSystemPlaylists()
        val visible = addAudioMedia("visible", 100)
        val secret = addAudioMedia("secret", 40)
        val secretPlaylistId = facade.addPlaylist(title = "비밀", type = PlaylistType.SECRET)
        facade.addTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(secret))

        val totalTracks = db.playlistTrackDao().getPlaylistTrackFullListFlow(totalId(), showSecretInTotal = false).first()
        assertEquals(listOf(visible), totalTracks.map { it.audioMediaEntity.id })

        val total = db.playlistDao().getPlaylistFlow(totalId()).first()!!
        assertEquals(1, total.audioMediaCount)
        assertEquals(100L, total.rawPlayTimeDuration)
        assertEquals(total, db.playlistDao().getPlaylistListFlow().first().first { it.id == totalId() })
    }

    @Test
    fun 비밀_곡을_보이게_하면_전체에_모두_나오고_다른_플레이리스트와_원본_값은_그대로다() = runTest {
        facade.ensureSystemPlaylists()
        val secret = addAudioMedia("secret", 40)
        val secretPlaylistId = facade.addPlaylist(title = "비밀", type = PlaylistType.SECRET)
        facade.addTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(secret))

        val totalTracks = db.playlistTrackDao().getPlaylistTrackFullListFlow(totalId(), showSecretInTotal = true).first()
        assertEquals(listOf(secret), totalTracks.map { it.audioMediaEntity.id })

        val secretTracks = db.playlistTrackDao().getPlaylistTrackFullListFlow(secretPlaylistId, showSecretInTotal = false).first()
        assertEquals(listOf(secret), secretTracks.map { it.audioMediaEntity.id })
        assertEquals(1, db.playlistDao().getPlaylistFlow(secretPlaylistId).first()!!.audioMediaCount)

        assertEquals(1, db.playlistDao().getPlaylistById(totalId()).audioMediaCount)
        assertEquals(listOf(secret), db.playlistTrackDao().getSecretAudioMediaIdListFlow().first())
    }
}
