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
import io.mockk.mockk
import kotlinx.coroutines.flow.first
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
    fun `addNewAudioMedia_실패시_롤백돼야한다`(): Unit = runTest {
        val targetFacade = AudioMediaFacade(
            db = db,
            audioMediaDao = audioMediaDao,
            playlistDao = playlistDao,
            playlistTrackDao = mockk<PlaylistTrackDao>(),
        )

        val result = runCatching {
            targetFacade.addNewAudioMedia(
                newAudioMediaEntity.copy(
                    rawAudioDurationSeconds = 100L,
                    imageFileNameWithExt = "test1",
                )
            )
        }

        assert(result.isFailure)
        assert(audioMediaDao.getAudioMediaListFlow().first().isEmpty())
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

    @Test
    fun `addTrack_정상동작_테스트`(): Unit = runTest {
        val inputList = listOf(
            100L to "test1",
            200L to "test2",
            300L to "test3",
        )
        val targetList = inputList.map {
            audioMediaFacade.addNewAudioMedia(
                newAudioMediaEntity.copy(
                    rawAudioDurationSeconds = it.first,
                    imageFileNameWithExt = it.second,
                )
            ).first
        }

        val newPlaylistId = playlistDao.insertPlaylist(newPlaylistEntity).toInt()
        audioMediaFacade.addTrack(newPlaylistId, targetList)

        assertValidPlaylist(
            playlistId = Playlist.TOTAL,
            expectedTrackCount = inputList.size,
            expectedDuration = inputList.sumOf { it.first },
        )
        assertValidPlaylist(
            playlistId = Playlist.UNCATEGORIZED,
            expectedTrackCount = 0,
            expectedDuration = 0L,
        )
        assertValidPlaylist(
            playlistId = newPlaylistId,
            expectedTrackCount = inputList.size,
            expectedDuration = inputList.sumOf { it.first },
        )
    }

    @Test
    fun `deletePlaylist_정상동작_테스트`(): Unit = runTest {
        val inputList = listOf(
            100L to "test1",
            200L to "test2",
            300L to "test3",
        )
        val targetList = inputList.map {
            audioMediaFacade.addNewAudioMedia(
                newAudioMediaEntity.copy(
                    rawAudioDurationSeconds = it.first,
                    imageFileNameWithExt = it.second,
                )
            ).first
        }

        val newPlaylistId = playlistDao.insertPlaylist(newPlaylistEntity).toInt()
        audioMediaFacade.addTrack(newPlaylistId, targetList)
        audioMediaFacade.deletePlaylist(newPlaylistId)

        assert(runCatching { playlistDao.getPlaylistById(newPlaylistId) }.getOrNull() == null)
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
    fun `deleteTrack_정상동작_테스트`(): Unit = runTest {
        val inputList = listOf(
            100L to "test1",
            200L to "test2",
            300L to "test3",
        )
        val takeCountToDelete = 1
        val targetList = inputList.map {
            audioMediaFacade.addNewAudioMedia(
                newAudioMediaEntity.copy(
                    rawAudioDurationSeconds = it.first,
                    imageFileNameWithExt = it.second,
                )
            ).first
        }

        val newPlaylistId = playlistDao.insertPlaylist(newPlaylistEntity).toInt()
        audioMediaFacade.addTrack(newPlaylistId, targetList)
        audioMediaFacade.deleteTrack(newPlaylistId, targetList.take(takeCountToDelete))

        assertValidPlaylist(
            playlistId = newPlaylistId,
            expectedTrackCount = targetList.size - takeCountToDelete,
            expectedDuration = inputList.drop(takeCountToDelete).sumOf { it.first },
        )
        assertValidPlaylist(
            playlistId = Playlist.TOTAL,
            expectedTrackCount = inputList.size,
            expectedDuration = inputList.sumOf { it.first },
        )
        assertValidPlaylist(
            playlistId = Playlist.UNCATEGORIZED,
            expectedTrackCount = takeCountToDelete,
            expectedDuration = inputList.take(takeCountToDelete).sumOf { it.first },
        )
    }

    @Test
    fun `updateTrackSequence_순서가_더_큰쪽으로_옮겼을때_정상동작_테스트`(): Unit = runTest {
        val initIndex = 0
        val moveToIndex = 2
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

        val prevTotalTrack = trackDao.getPlaylistTrackFullListFlow(Playlist.TOTAL).first().sortedBy { it.playlistTrackEntity.sequence }
        audioMediaFacade.updateTrackSequence(
            Playlist.TOTAL,
            prevTotalTrack[initIndex].audioMediaEntity.id,
            prevTotalTrack[initIndex].playlistTrackEntity.sequence,
            prevTotalTrack[moveToIndex].playlistTrackEntity.sequence
        )
        val currentTotalTrack = trackDao.getPlaylistTrackFullListFlow(Playlist.TOTAL).first().sortedBy { it.playlistTrackEntity.sequence }

        assert(prevTotalTrack[initIndex].playlistTrackEntity.audioMediaId == currentTotalTrack[moveToIndex].playlistTrackEntity.audioMediaId)
    }

    @Test
    fun `updateTrackSequence_순서가_더_작은쪽으로_옮겼을때_정상동작_테스트`(): Unit = runTest {
        val initIndex = 2
        val moveToIndex = 0
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

        val prevTotalTrack = trackDao.getPlaylistTrackFullListFlow(Playlist.TOTAL).first().sortedBy { it.playlistTrackEntity.sequence }
        audioMediaFacade.updateTrackSequence(
            Playlist.TOTAL,
            prevTotalTrack[initIndex].audioMediaEntity.id,
            prevTotalTrack[initIndex].playlistTrackEntity.sequence,
            prevTotalTrack[moveToIndex].playlistTrackEntity.sequence
        )
        val currentTotalTrack = trackDao.getPlaylistTrackFullListFlow(Playlist.TOTAL).first().sortedBy { it.playlistTrackEntity.sequence }

        assert(prevTotalTrack[initIndex].playlistTrackEntity.audioMediaId == currentTotalTrack[moveToIndex].playlistTrackEntity.audioMediaId)
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