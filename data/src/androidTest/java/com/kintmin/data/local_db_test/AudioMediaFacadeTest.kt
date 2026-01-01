package com.kintmin.data.local_db_test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.di.TestDatabase
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_db.model.PlaylistEntity
import com.kintmin.domain.playlist.model.Playlist
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AudioMediaFacadeTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    @TestDatabase
    lateinit var db: JellyTubeDatabase

    lateinit var audioMediaDao: AudioMediaDao
    lateinit var playlistDao: PlaylistDao
    lateinit var trackDao: PlaylistTrackDao
    lateinit var audioMediaFacade: AudioMediaFacade

    private val newAudioMediaEntity = AudioMediaEntity(
        source = "",
        name = "",
        artist = "",
        description = "",
        audioFileNameWithExt = "",
    )

    private val newPlaylistEntity = PlaylistEntity(
        name = "",
        description = "",
        audioMediaCount = 0,
        rawPlayTimeDuration = 0L,
        isCustomImage = false,
    )

    @Before
    fun setup() {
        hiltRule.inject()

        audioMediaDao = db.audioMediaDao()
        playlistDao = db.playlistDao()
        trackDao = db.playlistTrackDao()
        audioMediaFacade = AudioMediaFacade(
            db = db,
            audioMediaDao = audioMediaDao,
            playlistDao = playlistDao,
            playlistTrackDao = trackDao,
        )
    }

    @After
    fun tearDown() {
        db.clearAllTables()
        db.close()
    }

    @Test
    fun `addNewAudioMedia_정상동작_테스트`(): Unit = runTest {
        val inputList = listOf(
            100L to "test1",
            200L to "test2",
            300L to "test3",
        )

        inputList.forEach {
            audioMediaFacade.addNewAudioMedia(
                newAudioMediaEntity.copy(
                    rawAudioDurationSeconds = it.first,
                    imageFileNameWithExt = it.second,
                )
            )
        }

        assertValidPlaylist(
            playlistId = Playlist.TOTAL,
            expectedTrackCount = inputList.size,
            expectedDuration = inputList.sumOf { it.first },
        )
        assertValidPlaylist(
            playlistId = Playlist.UNCATEGORIZED,
            expectedTrackCount = inputList.size,
            expectedDuration = inputList.sumOf { it.first },
        )
    }

    @Test
    fun `deleteAudioMedia_정상동작_테스트`(): Unit = runTest {
        val (newAudioMediaId, _) = audioMediaFacade.addNewAudioMedia(
            newAudioMediaEntity.copy(
                rawAudioDurationSeconds = 300L,
                imageFileNameWithExt = "test",
            )
        )

        audioMediaFacade.deleteAudioMedia(newAudioMediaId)

        assertValidPlaylist(
            playlistId = Playlist.TOTAL,
            expectedTrackCount = 0,
            expectedDuration = 0L,
        )
        assertValidPlaylist(
            playlistId = Playlist.UNCATEGORIZED,
            expectedTrackCount = 0,
            expectedDuration = 0L,
        )
    }

    private suspend fun assertValidPlaylist(
        playlistId: Int,
        expectedTrackCount: Int,
        expectedDuration: Long,
        expectedImage: String? = null,
    ) {
        val playlist = playlistDao.getPlaylistById(playlistId)

        assert(expectedTrackCount == playlist.audioMediaCount)
        assert(expectedTrackCount == trackDao.getPlaylistTrackCount(playlistId))
        assert(expectedDuration == playlist.rawPlayTimeDuration)
        if (playlist.isCustomImage) {
            assert(expectedImage == playlist.imageFileNameWithExt)
        } else {
            val firstAudioTrack = runCatching { trackDao.getFirstAudioMediaWithNoLock(playlistId) }.getOrNull()
            assert(firstAudioTrack?.audioMediaEntity?.imageFileNameWithExt == playlist.imageFileNameWithExt)
        }
    }
}