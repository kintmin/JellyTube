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

    private suspend fun addAudioMedia(name: String, durationSeconds: Long = 10): Int =
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

    private suspend fun totalId(): Int = db.playlistDao().getPlaylistIdByType(PlaylistType.TOTAL.name)!!

    private suspend fun favoriteId(): Int = db.playlistDao().getPlaylistIdByType(PlaylistType.FAVORITE.name)!!

    private suspend fun secretFavoriteId(): Int? = db.playlistDao().getPlaylistIdByType(PlaylistType.SECRET_FAVORITE.name)

    private suspend fun trackIds(playlistId: Int, showSecretInTotal: Boolean = false): List<Int> =
        db.playlistTrackDao().getPlaylistTrackFullListFlow(playlistId, showSecretInTotal)
            .first().map { it.audioMediaEntity.id }

    @Test
    fun 전체의_곡_목록과_곡_수_길이에서_비밀_곡을_뺀다() = runTest {
        facade.ensureSystemPlaylists()
        val visible = addAudioMedia("visible", 100)
        val secret = addAudioMedia("secret", 40)
        val secretPlaylistId = facade.addPlaylist(title = "비밀", type = PlaylistType.SECRET)
        facade.addTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(secret))

        assertEquals(listOf(visible), trackIds(totalId()))

        val total = db.playlistDao().getPlaylistFlow(totalId()).first()!!
        assertEquals(1, total.audioMediaCount)
        assertEquals(100L, total.rawPlayTimeDuration)
        assertEquals(total, db.playlistDao().getPlaylistListFlow().first().first { it.id == totalId() })
    }

    @Test
    fun 전체의_썸네일은_비밀이_아닌_첫_곡의_것이고_모두_비밀이면_없다() = runTest {
        facade.ensureSystemPlaylists()
        val secret = addAudioMedia("secret")
        val secretPlaylistId = facade.addPlaylist(title = "비밀", type = PlaylistType.SECRET)
        facade.addTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(secret))

        assertEquals("secret.webp", db.playlistDao().getPlaylistById(totalId()).imageFileNameWithExt)
        assertEquals(null, db.playlistDao().getPlaylistFlow(totalId()).first()!!.imageFileNameWithExt)

        addAudioMedia("visible")

        assertEquals("visible.webp", db.playlistDao().getPlaylistFlow(totalId()).first()!!.imageFileNameWithExt)
        assertEquals("secret.webp", db.playlistDao().getPlaylistFlow(secretPlaylistId).first()!!.imageFileNameWithExt)
    }

    @Test
    fun 비밀_곡을_보이게_하면_전체에_모두_나오고_다른_플레이리스트와_원본_값은_그대로다() = runTest {
        facade.ensureSystemPlaylists()
        val secret = addAudioMedia("secret", 40)
        val secretPlaylistId = facade.addPlaylist(title = "비밀", type = PlaylistType.SECRET)
        facade.addTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(secret))

        assertEquals(listOf(secret), trackIds(totalId(), showSecretInTotal = true))

        assertEquals(listOf(secret), trackIds(secretPlaylistId))
        assertEquals(1, db.playlistDao().getPlaylistFlow(secretPlaylistId).first()!!.audioMediaCount)

        assertEquals(1, db.playlistDao().getPlaylistById(totalId()).audioMediaCount)
        assertEquals(listOf(secret), db.playlistTrackDao().getSecretAudioMediaIdListFlow().first())
    }

    @Test
    fun 비밀에_담으면_드러난_플레이리스트_연결을_모두_끊는다() = runTest {
        facade.ensureSystemPlaylists()
        val song = addAudioMedia("song")
        val userPlaylistId = facade.addPlaylist(title = "드림", type = PlaylistType.USER)
        facade.addTrack(playlistId = userPlaylistId, audioMediaIdList = listOf(song))
        facade.setFavorite(song, isFavorite = true)

        val secretPlaylistId = facade.addPlaylist(title = "비밀", type = PlaylistType.SECRET)
        val unlinkedCount = facade.addTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(song))

        assertEquals(1, unlinkedCount)
        assertEquals(emptyList<Int>(), trackIds(userPlaylistId))
        assertEquals(0, db.playlistDao().getPlaylistFlow(userPlaylistId).first()!!.audioMediaCount)
        assertEquals(emptyList<Int>(), trackIds(favoriteId()))
        assertEquals(0, db.playlistDao().getPlaylistFlow(favoriteId()).first()!!.audioMediaCount)
        assertEquals(emptyList<Int>(), trackIds(totalId()))
        assertEquals(listOf(song), trackIds(secretPlaylistId))
    }

    @Test
    fun 일반_플레이리스트에_담는_것은_다른_연결을_끊지_않는다() = runTest {
        facade.ensureSystemPlaylists()
        val song = addAudioMedia("song")
        val first = facade.addPlaylist(title = "드림", type = PlaylistType.USER)
        val second = facade.addPlaylist(title = "운동", type = PlaylistType.USER)
        facade.addTrack(playlistId = first, audioMediaIdList = listOf(song))

        val unlinkedCount = facade.addTrack(playlistId = second, audioMediaIdList = listOf(song))

        assertEquals(0, unlinkedCount)
        assertEquals(listOf(song), trackIds(first))
        assertEquals(listOf(song), trackIds(second))
    }

    @Test
    fun 비밀_곡의_찜은_일반_즐겨찾기가_아니라_비밀_찜으로_간다() = runTest {
        facade.ensureSystemPlaylists()
        val open = addAudioMedia("open")
        val secret = addAudioMedia("secret")
        val secretPlaylistId = facade.addPlaylist(title = "비밀", type = PlaylistType.SECRET)
        facade.addTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(secret))

        facade.setFavorite(open, isFavorite = true)
        facade.setFavorite(secret, isFavorite = true)

        assertEquals(listOf(open), trackIds(favoriteId()))
        assertEquals(listOf(secret), trackIds(secretFavoriteId()!!))
    }

    @Test
    fun 비밀에서_빼면_비밀_찜에서도_빠진다() = runTest {
        facade.ensureSystemPlaylists()
        val stays = addAudioMedia("stays")
        val leaves = addAudioMedia("leaves")
        val secretPlaylistId = facade.addPlaylist(title = "비밀", type = PlaylistType.SECRET)
        facade.addTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(stays, leaves))
        facade.setFavorite(stays, isFavorite = true)
        facade.setFavorite(leaves, isFavorite = true)

        facade.deleteTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(leaves))

        assertEquals(listOf(stays), trackIds(secretFavoriteId()!!))
        assertEquals(1, db.playlistDao().getPlaylistFlow(secretFavoriteId()!!).first()!!.audioMediaCount)
        // 비밀에서 나온 곡은 일반 즐겨찾기로 옮겨 가지 않는다. 다시 찜하면 그때 들어간다.
        assertEquals(emptyList<Int>(), trackIds(favoriteId()))
    }

    @Test
    fun 비밀_플레이리스트를_지우면_남은_곡의_비밀_찜도_정리된다() = runTest {
        facade.ensureSystemPlaylists()
        val song = addAudioMedia("song")
        val secretPlaylistId = facade.addPlaylist(title = "비밀", type = PlaylistType.SECRET)
        facade.addTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(song))
        facade.setFavorite(song, isFavorite = true)

        facade.deletePlaylist(secretPlaylistId)

        assertEquals(emptyList<Int>(), trackIds(secretFavoriteId()!!))
        assertEquals(listOf(song), trackIds(totalId()))
    }
}
