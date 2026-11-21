package com.kintmin.data.local_db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.model.AudioMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 실행: ./gradlew.bat :shared:data:connectedAndroidDeviceTest */
@RunWith(AndroidJUnit4::class)
class AudioMediaFacadeReorderTracksTest {

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

    private suspend fun addAudioMedia(name: String): Int = facade.addNewAudioMedia(
        newAudioMedia = AudioMediaEntity(
            source = "https://example.com/$name",
            name = name,
            artist = "",
            description = "",
            rawAudioDurationSeconds = 100,
            audioFileNameWithExt = "$name.m4a",
            imageFileNameWithExt = "$name.webp",
        ),
        requestedPlaylistId = null,
        shouldInsertAtTopOnDownload = false,
    ).audioMediaId

    private suspend fun orderOf(playlistId: Int): List<Int> =
        db.playlistTrackDao().getAll().filter { it.playlistId == playlistId }.sortedBy { it.sequence }.map { it.audioMediaId }

    @Test
    fun 곡_순서를_한_번에_다시_매기고_표지를_첫_곡으로_맞춘다() = runTest {
        val (a, b, c) = listOf(addAudioMedia("a"), addAudioMedia("b"), addAudioMedia("c"))
        val playlistId = facade.addPlaylist("목록")
        facade.addTrack(playlistId, listOf(a, b, c))

        facade.reorderTracks(playlistId, listOf(c, a, b))

        assertEquals(listOf(c, a, b), orderOf(playlistId))
        assertEquals("c.webp", db.playlistDao().getPlaylistById(playlistId).imageFileNameWithExt)
    }
}
