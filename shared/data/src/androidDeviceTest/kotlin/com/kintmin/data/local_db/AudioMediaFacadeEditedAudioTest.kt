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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 편집본 적용/해제 시 플레이리스트 재생시간 합계와 편집 상태(AUDIO_EDIT_PROJECT)가 어긋나지 않는지
 * 검증하는 계측 테스트.
 *
 * 이 합계(PLAYLIST.rawPlayTimeDuration)는 트랙을 넣고 뺄 때 가감되는 누적값이고 재계산 경로가 없어서,
 * 한 번 틀어지면 복구되지 않는다. 그래서 delta 보정이 이 기능의 유일한 영구 손상 지점이다.
 *
 * 실기기/에뮬레이터에서 실행해야 한다(Room + BundledSQLiteDriver).
 * 실행: ./gradlew.bat :shared:data:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class AudioMediaFacadeEditedAudioTest {

    private lateinit var db: JellyTubeDatabase
    private lateinit var facade: AudioMediaFacade

    private companion object {

        const val ORIGIN_DURATION_SECONDS = 300L
        const val EDITED_DURATION_SECONDS = 120L
        const val TIMELINE_JSON = "{\"version\":1,\"originDurationMs\":300000,\"endMs\":120000,\"clips\":[]}"
    }

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
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** 음원 1건을 추가하고, 그 음원이 들린 모든 플레이리스트 id를 돌려준다. */
    private suspend fun addAudioMedia(): Pair<Int, List<Int>> {
        val added = facade.addNewAudioMedia(
            newAudioMedia = AudioMediaEntity(
                source = "https://example.com/origin",
                name = "원본 음원",
                artist = "아티스트",
                description = "",
                rawAudioDurationSeconds = ORIGIN_DURATION_SECONDS,
                audioFileNameWithExt = "origin.m4a",
            ),
            requestedPlaylistId = null,
            shouldInsertAtTopOnDownload = false,
        )
        val linkedPlaylistIds = db.playlistTrackDao().getLinkedPlaylistIdList(added.audioMediaId)
        return added.audioMediaId to linkedPlaylistIds
    }

    private suspend fun playTimeDurationOf(playlistId: Int): Long =
        db.playlistDao().getPlaylistById(playlistId).rawPlayTimeDuration

    @Test
    fun 편집본을_적용하면_연결된_모든_플레이리스트의_재생시간이_편집본_길이로_보정된다() = runTest {
        val (audioMediaId, linkedPlaylistIds) = addAudioMedia()

        // 전체 + 미분류 두 곳에 들어간다
        assertEquals(2, linkedPlaylistIds.size)
        linkedPlaylistIds.forEach { playlistId ->
            assertEquals(ORIGIN_DURATION_SECONDS, playTimeDurationOf(playlistId))
        }

        facade.applyEditedAudio(
            audioMediaId = audioMediaId,
            editedAudioFileNameWithExt = "edited.m4a",
            editedAudioDurationSeconds = EDITED_DURATION_SECONDS,
            timelineJson = TIMELINE_JSON,
        )

        linkedPlaylistIds.forEach { playlistId ->
            assertEquals(
                "편집본 길이로 보정돼야 함",
                EDITED_DURATION_SECONDS,
                playTimeDurationOf(playlistId),
            )
        }
    }

    @Test
    fun 편집본을_해제하면_재생시간이_원본_길이로_복원된다() = runTest {
        val (audioMediaId, linkedPlaylistIds) = addAudioMedia()

        facade.applyEditedAudio(
            audioMediaId = audioMediaId,
            editedAudioFileNameWithExt = "edited.m4a",
            editedAudioDurationSeconds = EDITED_DURATION_SECONDS,
            timelineJson = TIMELINE_JSON,
        )
        facade.clearEditedAudio(audioMediaId = audioMediaId)

        linkedPlaylistIds.forEach { playlistId ->
            assertEquals(
                "원본 길이로 복원돼야 함",
                ORIGIN_DURATION_SECONDS,
                playTimeDurationOf(playlistId),
            )
        }
    }

    @Test
    fun 편집본을_여러_번_덮어써도_재생시간이_누적되지_않는다() = runTest {
        val (audioMediaId, linkedPlaylistIds) = addAudioMedia()

        facade.applyEditedAudio(audioMediaId, "edited1.m4a", 200L, TIMELINE_JSON)
        facade.applyEditedAudio(audioMediaId, "edited2.m4a", 90L, TIMELINE_JSON)
        facade.applyEditedAudio(audioMediaId, "edited3.m4a", EDITED_DURATION_SECONDS, TIMELINE_JSON)

        linkedPlaylistIds.forEach { playlistId ->
            assertEquals(
                "마지막 편집본 길이만 반영돼야 함",
                EDITED_DURATION_SECONDS,
                playTimeDurationOf(playlistId),
            )
        }
    }

    @Test
    fun 편집본이_있는_음원을_플레이리스트에서_빼면_편집본_길이만큼_빠진다() = runTest {
        val (audioMediaId, linkedPlaylistIds) = addAudioMedia()
        val userPlaylistId = facade.addPlaylist(title = "사용자 플레이리스트")

        facade.applyEditedAudio(
            audioMediaId = audioMediaId,
            editedAudioFileNameWithExt = "edited.m4a",
            editedAudioDurationSeconds = EDITED_DURATION_SECONDS,
            timelineJson = TIMELINE_JSON,
        )
        facade.addTrack(playlistId = userPlaylistId, audioMediaIdList = listOf(audioMediaId))

        assertEquals(EDITED_DURATION_SECONDS, playTimeDurationOf(userPlaylistId))

        facade.deleteTrack(playlistId = userPlaylistId, audioMediaIdList = listOf(audioMediaId))

        assertEquals(
            "편집본 길이로 가감돼야 0이 됨",
            0L,
            playTimeDurationOf(userPlaylistId),
        )
        // 미분류로 되돌아간 뒤에도 전체 플레이리스트 합계는 편집본 길이를 유지한다
        linkedPlaylistIds.forEach { playlistId ->
            assertEquals(EDITED_DURATION_SECONDS, playTimeDurationOf(playlistId))
        }
    }

    private suspend fun timelineJsonOf(audioMediaId: Int): String? =
        db.audioEditProjectDao().getByAudioMediaId(audioMediaId)?.timelineJson

    @Test
    fun 편집본을_적용하면_편집_상태가_저장되고_다시_적용하면_덮어쓴다() = runTest {
        val (audioMediaId, _) = addAudioMedia()

        facade.applyEditedAudio(audioMediaId, "edited1.m4a", 200L, "{\"first\":true}")
        assertEquals("{\"first\":true}", timelineJsonOf(audioMediaId))

        facade.applyEditedAudio(audioMediaId, "edited2.m4a", 90L, TIMELINE_JSON)
        assertEquals(TIMELINE_JSON, timelineJsonOf(audioMediaId))
    }

    @Test
    fun 편집본을_해제하면_편집_상태도_지워진다() = runTest {
        val (audioMediaId, _) = addAudioMedia()

        facade.applyEditedAudio(audioMediaId, "edited.m4a", EDITED_DURATION_SECONDS, TIMELINE_JSON)
        facade.clearEditedAudio(audioMediaId = audioMediaId)

        assertNull(timelineJsonOf(audioMediaId))
    }

    @Test
    fun 음원을_지우면_편집_상태도_같이_지워진다() = runTest {
        val (audioMediaId, _) = addAudioMedia()

        facade.applyEditedAudio(audioMediaId, "edited.m4a", EDITED_DURATION_SECONDS, TIMELINE_JSON)
        facade.deleteAudioMedia(audioMediaId = audioMediaId)

        // 외래키 ON DELETE CASCADE
        assertNull(timelineJsonOf(audioMediaId))
    }
}
