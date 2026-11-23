package com.kintmin.data.local_db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.domain.audio_track.model.SecretTrackFilter
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

    private suspend fun trackIds(playlistId: Int, filter: SecretTrackFilter): List<Int> =
        db.playlistTrackDao().getPlaylistTrackFullListFlow(playlistId, filter.name)
            .first().map { it.audioMediaEntity.id }

    @Test
    fun 전체의_곡_목록과_곡_수_길이에서_비밀_곡을_뺀다() = runTest {
        facade.ensureSystemPlaylists()
        val visible = addAudioMedia("visible", 100)
        val secret = addAudioMedia("secret", 40)
        val secretPlaylistId = facade.addPlaylist(title = "비밀", type = PlaylistType.SECRET)
        facade.addTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(secret))

        assertEquals(listOf(visible), trackIds(totalId(), SecretTrackFilter.EXCLUDE))

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

        assertEquals(listOf(secret), trackIds(totalId(), SecretTrackFilter.INCLUDE))

        assertEquals(listOf(secret), trackIds(secretPlaylistId, SecretTrackFilter.EXCLUDE))
        assertEquals(1, db.playlistDao().getPlaylistFlow(secretPlaylistId).first()!!.audioMediaCount)

        assertEquals(1, db.playlistDao().getPlaylistById(totalId()).audioMediaCount)
        assertEquals(listOf(secret), db.playlistTrackDao().getSecretAudioMediaIdListFlow().first())
    }

    @Test
    fun 즐겨찾기도_비밀_곡을_빼되_거기_담긴_것만_뺀다() = runTest {
        facade.ensureSystemPlaylists()
        val visible = addAudioMedia("visible", 100)
        val secretFavorite = addAudioMedia("secretFavorite", 40)
        val secretElsewhere = addAudioMedia("secretElsewhere", 30)
        val secretPlaylistId = facade.addPlaylist(title = "비밀", type = PlaylistType.SECRET)
        facade.addTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(secretFavorite, secretElsewhere))
        facade.setFavorite(visible, isFavorite = true)
        facade.setFavorite(secretFavorite, isFavorite = true)

        assertEquals(listOf(visible), trackIds(favoriteId(), SecretTrackFilter.EXCLUDE))

        // 비밀 곡 전체를 빼면 즐겨찾기에 없는 secretElsewhere 까지 빠져 곡 수가 음수로 내려간다.
        val favorite = db.playlistDao().getPlaylistFlow(favoriteId()).first()!!
        assertEquals(1, favorite.audioMediaCount)
        assertEquals(100L, favorite.rawPlayTimeDuration)
    }

    @Test
    fun 비밀만_보기는_그_플레이리스트에_담긴_비밀_곡만_돌려준다() = runTest {
        facade.ensureSystemPlaylists()
        val visible = addAudioMedia("visible")
        val secretFavorite = addAudioMedia("secretFavorite")
        val secretElsewhere = addAudioMedia("secretElsewhere")
        val secretPlaylistId = facade.addPlaylist(title = "비밀", type = PlaylistType.SECRET)
        facade.addTrack(playlistId = secretPlaylistId, audioMediaIdList = listOf(secretFavorite, secretElsewhere))
        facade.setFavorite(visible, isFavorite = true)
        facade.setFavorite(secretFavorite, isFavorite = true)

        assertEquals(listOf(secretFavorite), trackIds(favoriteId(), SecretTrackFilter.ONLY))
    }
}
