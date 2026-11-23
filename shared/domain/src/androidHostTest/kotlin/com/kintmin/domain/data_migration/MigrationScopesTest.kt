package com.kintmin.domain.data_migration

import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationEditProject
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationScope
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationStep
import com.kintmin.domain.data_migration.model.MigrationTrack
import com.kintmin.domain.playlist.model.PlaylistType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationScopesTest {

    // 전체 1 · 미분류 2 · 즐겨찾기 3 · 내 플레이리스트 4 · 비밀 5 · 비밀 찜 6
    // 곡 101 은 내 플레이리스트에, 102 는 미분류에, 103 은 비밀에만 담겨 있다.
    private val snapshot = MigrationSnapshot(
        audioMedias = listOf(audio(101), audio(102), audio(103)),
        playlists = listOf(
            playlist(1, PlaylistType.TOTAL),
            playlist(2, PlaylistType.UNCATEGORIZED),
            playlist(3, PlaylistType.FAVORITE),
            playlist(4, PlaylistType.USER, name = "운동할 때"),
            playlist(5, PlaylistType.SECRET, name = "나만 듣는 곡"),
            playlist(6, PlaylistType.SECRET_FAVORITE),
        ),
        tracks = listOf(
            MigrationTrack(1, 101, 1), MigrationTrack(1, 102, 2),
            MigrationTrack(2, 102, 1),
            MigrationTrack(3, 101, 1),
            MigrationTrack(4, 101, 1),
            MigrationTrack(5, 103, 1),
            MigrationTrack(6, 103, 1),
        ),
        editProjects = listOf(MigrationEditProject(101, "{}"), MigrationEditProject(103, "{}")),
        steps = listOf(MigrationStep(1L, 10L)),
    )

    @Test
    fun `비밀모드가 켜진 동기화는 스냅샷을 그대로 내보낸다`() {
        val narrowed = MigrationScopes.narrow(snapshot, MigrationScope.Everything(includesSecret = true))

        assertEquals(snapshot, narrowed)
    }

    @Test
    fun `비밀모드가 꺼진 동기화는 비밀 플레이리스트와 거기에만 담긴 곡을 뺀다`() {
        val narrowed = MigrationScopes.narrow(snapshot, MigrationScope.Everything(includesSecret = false))

        assertEquals(listOf(1, 2, 3, 4), narrowed.playlists.map { it.id })
        assertEquals(listOf(101, 102), narrowed.audioMedias.map { it.id })
        // 전체 목록에 남은 비밀 곡도 따라가지 않는다 — 받는 기기의 전체에 드러난다.
        assertTrue(narrowed.tracks.none { it.audioMediaId == 103 })
        assertEquals(listOf(101), narrowed.editProjects.map { it.audioMediaId })
        assertEquals(snapshot.steps, narrowed.steps)
    }

    @Test
    fun `플레이리스트 공유는 고른 것과 그 안의 곡만 내보내고 걸음 기록은 두고 간다`() {
        val narrowed = MigrationScopes.narrow(snapshot, MigrationScope.Playlists(setOf(4)))

        assertEquals(listOf(4), narrowed.playlists.map { it.id })
        assertEquals(listOf(101), narrowed.audioMedias.map { it.id })
        assertEquals(listOf(MigrationTrack(4, 101, 1)), narrowed.tracks)
        assertEquals(emptyList<MigrationStep>(), narrowed.steps)
    }

    @Test
    fun `시스템 플레이리스트를 공유하면 받는 쪽에서 내 플레이리스트가 된다`() {
        val narrowed = MigrationScopes.narrow(snapshot, MigrationScope.Playlists(setOf(2, 3, 6)))

        assertEquals(
            listOf(
                PlaylistType.USER to "공유받은 미분류",
                PlaylistType.USER to "공유받은 즐겨찾기",
                PlaylistType.SECRET to "공유받은 찜한 비밀 곡",
            ),
            narrowed.playlists.map { it.type to it.name },
        )
    }

    @Test
    fun `내 플레이리스트와 비밀 플레이리스트는 이름과 종류가 그대로 간다`() {
        val narrowed = MigrationScopes.narrow(snapshot, MigrationScope.Playlists(setOf(4, 5)))

        assertEquals(
            listOf(PlaylistType.USER to "운동할 때", PlaylistType.SECRET to "나만 듣는 곡"),
            narrowed.playlists.map { it.type to it.name },
        )
    }

    @Test
    fun `곡 공유는 플레이리스트와 소속을 비워 보낸다`() {
        val narrowed = MigrationScopes.narrow(snapshot, MigrationScope.AudioMedias(setOf(102, 103)))

        assertEquals(listOf(102, 103), narrowed.audioMedias.map { it.id })
        assertEquals(emptyList<MigrationPlaylist>(), narrowed.playlists)
        assertEquals(emptyList<MigrationTrack>(), narrowed.tracks)
        assertEquals(emptyList<MigrationStep>(), narrowed.steps)
        assertEquals(listOf(103), narrowed.editProjects.map { it.audioMediaId })
    }

    private fun audio(id: Int) = MigrationAudioMedia(
        id = id,
        source = "https://youtu.be/$id",
        name = "곡$id",
        artist = "",
        description = "",
        rawAudioDurationSeconds = 100,
        audioFileName = "$id.m4a",
        imageFileName = null,
        lyricFileName = null,
        tjKaraokeNumber = null,
        editedAudioFileName = null,
        editedAudioDurationSeconds = null,
        createdTimeMillis = id.toLong(),
    )

    private fun playlist(id: Int, type: PlaylistType, name: String = type.defaultName) = MigrationPlaylist(
        id = id,
        name = name,
        description = "",
        customImageFileName = null,
        sequence = id,
        type = type,
        createdTimeMillis = 0,
    )
}
