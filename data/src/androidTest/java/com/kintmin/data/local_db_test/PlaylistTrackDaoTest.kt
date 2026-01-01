package com.kintmin.data.local_db_test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dao.PlaylistTrackDao
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.di.TestDatabase
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_db.model.PlaylistEntity
import com.kintmin.data.local_db.model.PlaylistTrackEntity
import com.kintmin.domain.playlist.model.Playlist
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class PlaylistTrackDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    @TestDatabase
    lateinit var db: JellyTubeDatabase

    lateinit var audioMediaDao: AudioMediaDao
    lateinit var playlistDao: PlaylistDao
    lateinit var trackDao: PlaylistTrackDao

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
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert_시_외래키_제약조건_일치할때_정상작동해야한다`(): Unit = runBlocking {
        val audioMediaId = audioMediaDao.insertAudioMedia(newAudioMediaEntity).toInt()
        val playlistId = playlistDao.insertPlaylist(newPlaylistEntity).toInt()

        val result = runCatching {
            trackDao.insertPlaylistTrackList(
                listOf(PlaylistTrackEntity(playlistId, audioMediaId, 1))
            )
        }

        assert(result.isSuccess)
    }

    @Test
    fun `insert_시_외래키에_맞는_키가없을때_에러나야한다`(): Unit = runBlocking {
        val result = runCatching {
            trackDao.insertPlaylistTrackList(
                listOf(PlaylistTrackEntity(0, 0, 1))
            )
        }

        assert(result.isFailure)
    }

    @Test
    fun `insert_시_PK제약에_어긋나면_replace된다`(): Unit = runBlocking {
        val audioMediaId = audioMediaDao.insertAudioMedia(newAudioMediaEntity).toInt()
        val playlistId = playlistDao.insertPlaylist(newPlaylistEntity).toInt()

        trackDao.insertPlaylistTrackList(
            listOf(
                PlaylistTrackEntity(playlistId, audioMediaId, 1),
                PlaylistTrackEntity(playlistId, audioMediaId, 2)
            )
        )
        val count = trackDao.getPlaylistTrackCount(playlistId)
        val resultEntity = trackDao.getFirstAudioMediaWithNoLock(playlistId)

        assert(count == 1)
        assert(resultEntity.playlistTrackEntity.sequence == 2)
    }

    @Test
    fun `getPlaylistTrackFullListFlow_시_미디어나_플레이리스트_변경을_모두감지해야한다`(): Unit = runBlocking {
        val audioMediaId = audioMediaDao.insertAudioMedia(newAudioMediaEntity).toInt()
        val playlistId = playlistDao.insertPlaylist(newPlaylistEntity).toInt()

        trackDao.insertPlaylistTrackList(
            listOf(PlaylistTrackEntity(playlistId, audioMediaId, 1))
        )
        launch {
            delay(200)
            playlistDao.updatePlaylist(playlistId, name = "name")
            delay(200)
            audioMediaDao.updateAudioMedia(audioMediaId, name = "name")
        }

        launch {
            trackDao.getPlaylistTrackFullListFlow(playlistId).take(3).collect { entity ->
                assert(entity.first().playlistTrackEntity.sequence == 1)
                assert(entity.first().playlistEntity == playlistDao.getPlaylistById(playlistId))
                assert(entity.first().audioMediaEntity == audioMediaDao.getDataById(audioMediaId))
            }
        }
    }

    @Test
    fun `높은_순서로_변경_시_updateSequence_순서보장이_돼야한다`(): Unit = runBlocking {
        val audioMediaId1 = audioMediaDao.insertAudioMedia(newAudioMediaEntity).toInt()
        val audioMediaId2 = audioMediaDao.insertAudioMedia(newAudioMediaEntity).toInt()
        val playlistId = playlistDao.insertPlaylist(newPlaylistEntity).toInt()

        trackDao.insertPlaylistTrackList(
            listOf(
                PlaylistTrackEntity(playlistId, audioMediaId1, 1),
                PlaylistTrackEntity(playlistId, audioMediaId2, 2),
            )
        )
        trackDao.updateSequence(playlistId, audioMediaId1, 1, 2)

        val currentAudioMediaList = trackDao.getPlaylistTrackFullListFlow(playlistId).first().sortedBy { it.playlistTrackEntity.sequence }
        assert(currentAudioMediaList[0].audioMediaEntity.id == audioMediaId2)
        assert(currentAudioMediaList[1].audioMediaEntity.id == audioMediaId1)
    }

    @Test
    fun `낮은_순서로_변경_시_updateSequence_순서보장이_돼야한다`(): Unit = runBlocking {
        val audioMediaId1 = audioMediaDao.insertAudioMedia(newAudioMediaEntity).toInt()
        val audioMediaId2 = audioMediaDao.insertAudioMedia(newAudioMediaEntity).toInt()
        val playlistId = playlistDao.insertPlaylist(newPlaylistEntity).toInt()

        trackDao.insertPlaylistTrackList(
            listOf(
                PlaylistTrackEntity(playlistId, audioMediaId1, 1),
                PlaylistTrackEntity(playlistId, audioMediaId2, 2),
            )
        )
        trackDao.updateSequence(playlistId, audioMediaId2, 2, 1)

        val currentAudioMediaList = trackDao.getPlaylistTrackFullListFlow(playlistId).first().sortedBy { it.playlistTrackEntity.sequence }
        assert(currentAudioMediaList[0].audioMediaEntity.id == audioMediaId2)
        assert(currentAudioMediaList[1].audioMediaEntity.id == audioMediaId1)
    }

    @Test
    fun `값이_없어도_getMaxSequence는_0을_반환해야한다`(): Unit = runBlocking {
        val maxSequence = trackDao.getMaxSequence(Playlist.TOTAL)
        assert(maxSequence == 0)
    }
}