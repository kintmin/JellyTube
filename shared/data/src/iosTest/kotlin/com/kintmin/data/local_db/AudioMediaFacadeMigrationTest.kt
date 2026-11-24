package com.kintmin.data.local_db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kintmin.data.local_db.dao_facade.AudioMediaFacade
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_db.model.StepEntity
import com.kintmin.domain.data_migration.MigrationPlanner
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationEditProject
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationStep
import com.kintmin.domain.data_migration.model.MigrationTrack
import com.kintmin.domain.playlist.model.PlaylistType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioMediaFacadeMigrationTest {

    private lateinit var db: JellyTubeDatabase
    private lateinit var facade: AudioMediaFacade

    @BeforeTest
    fun setup() {
        db = Room.inMemoryDatabaseBuilder<JellyTubeDatabase>()
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

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private fun audio(id: Int, source: String) = MigrationAudioMedia(
        id = id, source = source, name = "곡$id", artist = "", description = "", rawAudioDurationSeconds = 100,
        audioFileName = "$id.m4a", imageFileName = "$id.jpg", lyricFileName = null, tjKaraokeNumber = null,
        editedAudioFileName = null, editedAudioDurationSeconds = null, createdTimeMillis = id.toLong(),
    )

    private val sourceSnapshot = MigrationSnapshot(
        audioMedias = listOf(
            audio(101, "https://youtu.be/old?si=x"),
            audio(102, "https://youtu.be/new").copy(editedAudioFileName = "102-edit.m4a", editedAudioDurationSeconds = 90),
        ),
        playlists = listOf(
            MigrationPlaylist(11, "전체", "", null, 0, PlaylistType.TOTAL, 0),
            MigrationPlaylist(12, "미분류", "", null, 0, PlaylistType.UNCATEGORIZED, 0),
            MigrationPlaylist(13, "즐겨찾기", "", null, 0, PlaylistType.FAVORITE, 0),
            MigrationPlaylist(14, "드라이브", "설명", "p.jpg", 1, PlaylistType.USER, 0),
        ),
        tracks = listOf(
            MigrationTrack(11, 101, 1), MigrationTrack(11, 102, 2),
            MigrationTrack(14, 101, 1), MigrationTrack(14, 102, 2),
            MigrationTrack(13, 102, 1),
        ),
        editProjects = listOf(MigrationEditProject(102, "{\"version\":1}")),
        steps = listOf(MigrationStep(1, 999), MigrationStep(2, 5)),
    )

    @Test
    fun `계획을 반영하면 새 id 로 들어가고 합계와 미분류가 맞는다`() = runTest {
        facade.ensureSystemPlaylists()
        val existingId = facade.addNewAudioMedia(
            newAudioMedia = AudioMediaEntity(
                source = "https://www.youtube.com/watch?v=old",
                name = "내 곡",
                artist = "",
                description = "",
                rawAudioDurationSeconds = 100,
                audioFileNameWithExt = "mine.m4a",
            ),
            requestedPlaylistId = null,
            shouldInsertAtTopOnDownload = false,
        ).audioMediaId
        db.stepDao().insert(StepEntity(rawCreatedTime = 1, stepSensor = 100))

        facade.applyMigrationPlan(MigrationPlanner.plan(sourceSnapshot, facade.fetchMigrationSnapshot()))

        val after = facade.fetchMigrationSnapshot()
        val newAudio = after.audioMedias.single { it.id != existingId }
        assertEquals("https://youtu.be/new", newAudio.source)
        assertEquals("내 곡", after.audioMedias.single { it.id == existingId }.name)

        val playlists = db.playlistDao().getAll()
        val drive = playlists.single { it.name == "드라이브" }
        assertEquals(2, drive.audioMediaCount)
        // 편집본이 있는 곡은 편집본 길이(90)로 센다.
        assertEquals(190, drive.rawPlayTimeDuration)
        assertEquals("p.jpg", drive.imageFileNameWithExt)
        assertTrue(drive.isCustomImage)
        assertEquals(2, playlists.single { it.type == PlaylistType.TOTAL.name }.audioMediaCount)
        val uncategorized = playlists.single { it.type == PlaylistType.UNCATEGORIZED.name }
        assertEquals(0, uncategorized.audioMediaCount)
        assertTrue(after.tracks.none { it.playlistId == uncategorized.id })
        val favorite = playlists.single { it.type == PlaylistType.FAVORITE.name }
        assertEquals(listOf(newAudio.id), after.tracks.filter { it.playlistId == favorite.id }.map { it.audioMediaId })
        assertEquals(listOf(existingId, newAudio.id), after.tracks.filter { it.playlistId == drive.id }.sortedBy { it.sequence }.map { it.audioMediaId })

        assertEquals(listOf(MigrationEditProject(newAudio.id, "{\"version\":1}")), after.editProjects)
        assertEquals(listOf(MigrationStep(1, 100), MigrationStep(2, 5)), after.steps.sortedBy { it.createdTimeMillis })
    }

    /** 이 기기에 곡 하나를 만들어 돌려준다. */
    private suspend fun addMyAudio() = facade.addNewAudioMedia(
        newAudioMedia = AudioMediaEntity(
            source = "https://www.youtube.com/watch?v=a",
            name = "내 곡",
            artist = "",
            description = "",
            rawAudioDurationSeconds = 100,
            audioFileNameWithExt = "mine.m4a",
        ),
        requestedPlaylistId = null,
        shouldInsertAtTopOnDownload = false,
    ).audioMediaId

    private suspend fun playlistTypesOf(audioMediaId: Int): Set<PlaylistType> {
        val after = facade.fetchMigrationSnapshot()
        val typeById = after.playlists.associate { it.id to it.type }
        return after.tracks.filter { it.audioMediaId == audioMediaId }.mapNotNull { typeById[it.playlistId] }.toSet()
    }

    @Test
    fun `이전으로 비밀 플레이리스트에 담긴 곡은 드러난 플레이리스트와 일반 즐겨찾기에서 빠진다`() = runTest {
        facade.ensureSystemPlaylists()
        val mineId = addMyAudio()
        val workoutId = facade.addPlaylist("운동", PlaylistType.USER)
        facade.addTrack(workoutId, listOf(mineId))
        facade.setFavorite(mineId, isFavorite = true)

        // 보내는 쪽에서 같은 곡이 비밀 플레이리스트에 담겨 온다.
        val source = MigrationSnapshot(
            audioMedias = listOf(audio(101, "https://youtu.be/a")),
            playlists = listOf(
                MigrationPlaylist(11, "전체", "", null, 0, PlaylistType.TOTAL, 0),
                MigrationPlaylist(15, "비밀", "", null, 1, PlaylistType.SECRET, 0),
            ),
            tracks = listOf(MigrationTrack(11, 101, 1), MigrationTrack(15, 101, 1)),
            editProjects = emptyList(),
            steps = emptyList(),
        )

        facade.applyMigrationPlan(MigrationPlanner.plan(source, facade.fetchMigrationSnapshot()))

        assertEquals(setOf(PlaylistType.TOTAL, PlaylistType.SECRET), playlistTypesOf(mineId))
        val playlists = db.playlistDao().getAll()
        assertEquals(0, playlists.single { it.name == "운동" }.audioMediaCount)
        assertEquals(0, playlists.single { it.type == PlaylistType.FAVORITE.name }.audioMediaCount)
    }

    @Test
    fun `이미 비밀인 곡은 이전으로 드러난 플레이리스트나 일반 즐겨찾기에 담기지 않는다`() = runTest {
        facade.ensureSystemPlaylists()
        val mineId = addMyAudio()
        facade.addTrack(facade.addPlaylist("비밀", PlaylistType.SECRET), listOf(mineId))

        // 보내는 쪽에서 같은 곡은 비밀이 아니라 일반 즐겨찾기와 USER 플레이리스트에 있다.
        val source = MigrationSnapshot(
            audioMedias = listOf(audio(101, "https://youtu.be/a")),
            playlists = listOf(
                MigrationPlaylist(11, "전체", "", null, 0, PlaylistType.TOTAL, 0),
                MigrationPlaylist(13, "즐겨찾기", "", null, 0, PlaylistType.FAVORITE, 0),
                MigrationPlaylist(14, "운동", "", null, 1, PlaylistType.USER, 0),
            ),
            tracks = listOf(MigrationTrack(11, 101, 1), MigrationTrack(13, 101, 1), MigrationTrack(14, 101, 1)),
            editProjects = emptyList(),
            steps = emptyList(),
        )

        facade.applyMigrationPlan(MigrationPlanner.plan(source, facade.fetchMigrationSnapshot()))

        assertEquals(setOf(PlaylistType.TOTAL, PlaylistType.SECRET), playlistTypesOf(mineId))
        val playlists = db.playlistDao().getAll()
        assertEquals(0, playlists.single { it.name == "운동" }.audioMediaCount)
        assertEquals(0, playlists.single { it.type == PlaylistType.FAVORITE.name }.audioMediaCount)
    }

    @Test
    fun `반영한 뒤 다시 계획하면 할 일이 없다`() = runTest {
        facade.ensureSystemPlaylists()
        facade.applyMigrationPlan(MigrationPlanner.plan(sourceSnapshot, facade.fetchMigrationSnapshot()))

        val again = MigrationPlanner.plan(sourceSnapshot, facade.fetchMigrationSnapshot())

        assertTrue(again.newAudioMedias.isEmpty())
        assertTrue(again.newPlaylists.isEmpty())
        assertTrue(again.trackAdditions.isEmpty())
        assertTrue(again.uncategorizedRemovals.isEmpty())
    }
}
