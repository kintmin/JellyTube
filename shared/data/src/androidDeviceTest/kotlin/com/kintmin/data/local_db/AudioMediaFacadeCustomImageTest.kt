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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 실행: ./gradlew.bat :shared:data:connectedAndroidDeviceTest */
@RunWith(AndroidJUnit4::class)
class AudioMediaFacadeCustomImageTest {

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

    private suspend fun addAudioMedia(
        name: String,
        imageFileNameWithExt: String? = "$name.webp",
    ): Int = facade.addNewAudioMedia(
        newAudioMedia = AudioMediaEntity(
            source = "https://example.com/$name",
            name = name,
            artist = "",
            description = "",
            rawAudioDurationSeconds = 100,
            audioFileNameWithExt = "$name.m4a",
            imageFileNameWithExt = imageFileNameWithExt,
        ),
        requestedPlaylistId = null,
        shouldInsertAtTopOnDownload = false,
    ).audioMediaId

    private suspend fun playlistWithFirstTrack(audioMediaId: Int): Int =
        facade.addPlaylist(title = "목록", type = PlaylistType.USER).also { playlistId ->
            facade.addTrack(playlistId, listOf(audioMediaId))
        }

    @Test
    fun 고른_썸네일을_입혀도_정본은_남고_되돌리면_정본으로_간다() = runTest {
        val id = addAudioMedia("a")

        facade.applyCustomImage(id, "고른것.webp")

        db.audioMediaDao().getDataById(id).let { after ->
            assertEquals("a.webp", after.imageFileNameWithExt)
            assertEquals("고른것.webp", after.customImageFileNameWithExt)
        }

        assertEquals(listOf("고른것.webp"), facade.clearCustomImage(id))
        db.audioMediaDao().getDataById(id).let { after ->
            assertEquals("a.webp", after.imageFileNameWithExt)
            assertNull(after.customImageFileNameWithExt)
        }
    }

    @Test
    fun 고른_썸네일을_다른_것으로_바꾸면_먼저_고른_파일만_떨어져_나간다() = runTest {
        val id = addAudioMedia("a")
        facade.applyCustomImage(id, "첫번째.webp")

        assertEquals(listOf("첫번째.webp"), facade.applyCustomImage(id, "두번째.webp"))

        db.audioMediaDao().getDataById(id).let { after ->
            assertEquals("a.webp", after.imageFileNameWithExt)
            assertEquals("두번째.webp", after.customImageFileNameWithExt)
        }
    }

    @Test
    fun 정본을_다시_정하면_옛_정본과_고른_썸네일이_함께_떨어져_나간다() = runTest {
        val id = addAudioMedia("a")
        facade.applyCustomImage(id, "고른것.webp")

        val orphans = facade.replaceImage(id, "PC가보낸것.webp")

        assertEquals(setOf("a.webp", "고른것.webp"), orphans.toSet())
        db.audioMediaDao().getDataById(id).let { after ->
            assertEquals("PC가보낸것.webp", after.imageFileNameWithExt)
            assertNull(after.customImageFileNameWithExt)
        }
    }

    @Test
    fun 썸네일_없이_들어온_곡의_선택을_떼면_커버도_비운다() = runTest {
        val id = addAudioMedia("업로드", imageFileNameWithExt = null)
        val playlistId = playlistWithFirstTrack(id)

        facade.applyCustomImage(id, "고른것.webp")
        assertEquals("고른것.webp", db.playlistDao().getPlaylistById(playlistId).imageFileNameWithExt)

        facade.clearCustomImage(id)

        // 비우지 않으면 커버가 곧 지워질 파일을 가리킨 채로 남는다.
        assertNull(db.playlistDao().getPlaylistById(playlistId).imageFileNameWithExt)
    }

    @Test
    fun 플레이리스트_커버는_곡이_보여주는_썸네일을_따라간다() = runTest {
        val id = addAudioMedia("a")
        val playlistId = playlistWithFirstTrack(id)
        assertEquals("a.webp", db.playlistDao().getPlaylistById(playlistId).imageFileNameWithExt)

        facade.applyCustomImage(id, "고른것.webp")
        assertEquals("고른것.webp", db.playlistDao().getPlaylistById(playlistId).imageFileNameWithExt)

        facade.clearCustomImage(id)
        assertEquals("a.webp", db.playlistDao().getPlaylistById(playlistId).imageFileNameWithExt)
    }
}
