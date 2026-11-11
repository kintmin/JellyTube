package com.kintmin.domain.data_migration

import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationEditProject
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationRef.Existing
import com.kintmin.domain.data_migration.model.MigrationRef.Incoming
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationStep
import com.kintmin.domain.data_migration.model.MigrationTrack
import com.kintmin.domain.data_migration.model.MigrationTrackAddition
import com.kintmin.domain.playlist.model.PlaylistType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationPlannerTest {

    // 받는 기기 시스템 플레이리스트: 전체 1, 미분류 2, 즐겨찾기 3
    private val targetSystem = listOf(
        playlist(1, PlaylistType.TOTAL),
        playlist(2, PlaylistType.UNCATEGORIZED),
        playlist(3, PlaylistType.FAVORITE),
    )

    // 보내는 기기 시스템 플레이리스트: id 가 받는 쪽과 다르다
    private val sourceSystem = listOf(
        playlist(11, PlaylistType.TOTAL),
        playlist(12, PlaylistType.UNCATEGORIZED),
        playlist(13, PlaylistType.FAVORITE),
    )

    @Test
    fun `비어 있는 기기로 옮기면 전부 새로 들어가고 시스템 플레이리스트는 type 으로 맞춘다`() {
        val source = snapshot(
            audios = listOf(
                audio(101, "https://youtu.be/a", image = "a.jpg", lyric = "a.lrc", edited = "a-edit.m4a"),
                audio(102, "https://youtu.be/b"),
            ),
            playlists = sourceSystem + playlist(14, PlaylistType.USER, name = "드라이브", customImage = "p.jpg"),
            tracks = listOf(
                track(11, 101, 1), track(11, 102, 2),
                track(14, 101, 1),
                track(12, 102, 1),
                track(13, 102, 1),
            ),
            editProjects = listOf(MigrationEditProject(101, "{}")),
            steps = listOf(MigrationStep(1L, 10L)),
        )

        val plan = MigrationPlanner.plan(source, snapshot(playlists = targetSystem))

        assertEquals(listOf(101, 102), plan.newAudioMedias.map { it.id })
        assertEquals(listOf(14), plan.newPlaylists.map { it.id })
        assertEquals(
            listOf(
                MigrationTrackAddition(Existing(1), listOf(Incoming(101), Incoming(102))),
                MigrationTrackAddition(Existing(3), listOf(Incoming(102))),
                MigrationTrackAddition(Incoming(14), listOf(Incoming(101))),
                MigrationTrackAddition(Existing(2), listOf(Incoming(102))),
            ),
            plan.trackAdditions,
        )
        assertEquals(listOf(MigrationEditProject(101, "{}")), plan.editProjects)
        assertEquals(listOf(MigrationStep(1L, 10L)), plan.steps)
        assertEquals(
            setOf(
                MigrationFile(MigrationFileKind.AUDIO, "101.m4a"),
                MigrationFile(MigrationFileKind.AUDIO, "a-edit.m4a"),
                MigrationFile(MigrationFileKind.IMAGE, "a.jpg"),
                MigrationFile(MigrationFileKind.LYRIC, "a.lrc"),
                MigrationFile(MigrationFileKind.AUDIO, "102.m4a"),
                MigrationFile(MigrationFileKind.IMAGE, "p.jpg"),
            ),
            plan.requiredFiles,
        )
    }

    @Test
    fun `같은 곡이면 받는 기기 쪽을 남기고 파일과 편집 상태를 가져오지 않는다`() {
        val source = snapshot(
            audios = listOf(audio(101, "https://youtu.be/a?si=x", image = "src.jpg")),
            playlists = sourceSystem,
            tracks = listOf(track(11, 101, 1), track(12, 101, 1)),
            editProjects = listOf(MigrationEditProject(101, "{}")),
        )
        val target = snapshot(
            audios = listOf(audio(7, "https://www.youtube.com/watch?v=a")),
            playlists = targetSystem,
            tracks = listOf(track(1, 7, 1), track(2, 7, 1)),
        )

        val plan = MigrationPlanner.plan(source, target)

        assertTrue(plan.newAudioMedias.isEmpty())
        assertTrue(plan.trackAdditions.isEmpty())
        assertTrue(plan.editProjects.isEmpty())
        assertTrue(plan.requiredFiles.isEmpty())
    }

    @Test
    fun `같은 곡의 소속과 즐겨찾기는 합치고 USER 에 들어가면 미분류에서 뺀다`() {
        val source = snapshot(
            audios = listOf(audio(101, "https://youtu.be/a")),
            playlists = sourceSystem + playlist(14, PlaylistType.USER, name = "운동"),
            tracks = listOf(track(11, 101, 1), track(13, 101, 1), track(14, 101, 1)),
        )
        val target = snapshot(
            audios = listOf(audio(7, "https://youtu.be/a")),
            playlists = targetSystem,
            tracks = listOf(track(1, 7, 1), track(2, 7, 1)),
        )

        val plan = MigrationPlanner.plan(source, target)

        assertEquals(
            listOf(
                MigrationTrackAddition(Existing(3), listOf(Existing(7))),
                MigrationTrackAddition(Incoming(14), listOf(Existing(7))),
            ),
            plan.trackAdditions,
        )
        assertEquals(listOf(7), plan.uncategorizedRemovals)
    }

    @Test
    fun `이름이 같은 USER 플레이리스트는 합치고 받는 쪽에 여러 개면 순서상 첫 번째로 간다`() {
        val source = snapshot(
            audios = listOf(audio(101, "https://youtu.be/a")),
            playlists = sourceSystem + playlist(14, PlaylistType.USER, name = " 드라이브 "),
            tracks = listOf(track(11, 101, 1), track(14, 101, 1)),
        )
        val target = snapshot(
            playlists = targetSystem +
                playlist(21, PlaylistType.USER, name = "드라이브", sequence = 2) +
                playlist(22, PlaylistType.USER, name = "드라이브", sequence = 1),
        )

        val plan = MigrationPlanner.plan(source, target)

        assertTrue(plan.newPlaylists.isEmpty())
        assertTrue(MigrationTrackAddition(Existing(22), listOf(Incoming(101))) in plan.trackAdditions)
        assertTrue(plan.trackAdditions.none { it.playlist == Existing(2) })
    }

    @Test
    fun `보내는 쪽에 이름이 같은 플레이리스트가 여럿이면 하나로 만들어 합친다`() {
        val source = snapshot(
            audios = listOf(audio(101, "https://youtu.be/a"), audio(102, "https://youtu.be/b")),
            playlists = sourceSystem +
                playlist(14, PlaylistType.USER, name = "A", sequence = 1) +
                playlist(15, PlaylistType.USER, name = "A", sequence = 2),
            tracks = listOf(track(11, 101, 1), track(11, 102, 2), track(14, 101, 1), track(15, 102, 1)),
        )

        val plan = MigrationPlanner.plan(source, snapshot(playlists = targetSystem))

        assertEquals(listOf(14), plan.newPlaylists.map { it.id })
        assertTrue(MigrationTrackAddition(Incoming(14), listOf(Incoming(101), Incoming(102))) in plan.trackAdditions)
    }

    @Test
    fun `보내는 쪽 안의 중복 곡은 먼저 만든 곡 하나로 합친다`() {
        val source = snapshot(
            audios = listOf(
                audio(101, "https://youtu.be/a?si=1", createdTime = 20),
                audio(102, "https://youtu.be/a?si=2", createdTime = 10),
            ),
            playlists = sourceSystem + playlist(14, PlaylistType.USER, name = "A"),
            tracks = listOf(track(11, 101, 1), track(11, 102, 2), track(14, 101, 1), track(14, 102, 2)),
        )

        val plan = MigrationPlanner.plan(source, snapshot(playlists = targetSystem))

        assertEquals(listOf(102), plan.newAudioMedias.map { it.id })
        assertTrue(MigrationTrackAddition(Existing(1), listOf(Incoming(102))) in plan.trackAdditions)
        assertTrue(MigrationTrackAddition(Incoming(14), listOf(Incoming(102))) in plan.trackAdditions)
    }

    @Test
    fun `보내는 쪽 전체 목록에 빠진 새 곡도 전체에 넣는다`() {
        val source = snapshot(
            audios = listOf(audio(101, "https://youtu.be/a")),
            playlists = sourceSystem,
        )

        val plan = MigrationPlanner.plan(source, snapshot(playlists = targetSystem))

        assertTrue(MigrationTrackAddition(Existing(1), listOf(Incoming(101))) in plan.trackAdditions)
        assertTrue(MigrationTrackAddition(Existing(2), listOf(Incoming(101))) in plan.trackAdditions)
    }

    @Test
    fun `편집본 파일이 없는 곡은 편집 상태도 옮기지 않는다`() {
        val source = snapshot(
            audios = listOf(audio(101, "https://youtu.be/a", edited = "a-edit.m4a"), audio(102, "https://youtu.be/b")),
            playlists = sourceSystem,
            editProjects = listOf(MigrationEditProject(101, "{}"), MigrationEditProject(102, "{}")),
        )

        val plan = MigrationPlanner.plan(source, snapshot(playlists = targetSystem))

        assertEquals(listOf(MigrationEditProject(101, "{}")), plan.editProjects)
    }

    @Test
    fun `이미 옮겨 온 데이터를 다시 옮기면 걸음 수 말고는 할 일이 없다`() {
        val data = snapshot(
            audios = listOf(audio(101, "https://youtu.be/a"), audio(102, "https://youtu.be/b")),
            playlists = sourceSystem + playlist(14, PlaylistType.USER, name = "A"),
            tracks = listOf(
                track(11, 101, 1), track(11, 102, 2),
                track(14, 101, 1), track(12, 102, 1), track(13, 101, 1),
            ),
            editProjects = listOf(MigrationEditProject(101, "{}")),
            steps = listOf(MigrationStep(1L, 10L)),
        )

        val plan = MigrationPlanner.plan(data, data)

        assertTrue(plan.newAudioMedias.isEmpty())
        assertTrue(plan.newPlaylists.isEmpty())
        assertTrue(plan.trackAdditions.isEmpty())
        assertTrue(plan.uncategorizedRemovals.isEmpty())
        assertTrue(plan.editProjects.isEmpty())
        assertTrue(plan.requiredFiles.isEmpty())
    }

    private fun snapshot(
        audios: List<MigrationAudioMedia> = emptyList(),
        playlists: List<MigrationPlaylist> = emptyList(),
        tracks: List<MigrationTrack> = emptyList(),
        editProjects: List<MigrationEditProject> = emptyList(),
        steps: List<MigrationStep> = emptyList(),
    ) = MigrationSnapshot(audios, playlists, tracks, editProjects, steps)

    private fun audio(
        id: Int,
        source: String,
        image: String? = null,
        lyric: String? = null,
        edited: String? = null,
        createdTime: Long = id.toLong(),
    ) = MigrationAudioMedia(
        id = id,
        source = source,
        name = "곡$id",
        artist = "",
        description = "",
        rawAudioDurationSeconds = 100,
        audioFileName = "$id.m4a",
        imageFileName = image,
        lyricFileName = lyric,
        tjKaraokeNumber = null,
        editedAudioFileName = edited,
        editedAudioDurationSeconds = edited?.let { 90 },
        createdTimeMillis = createdTime,
    )

    private fun playlist(
        id: Int,
        type: PlaylistType,
        name: String = type.defaultName,
        sequence: Int = if (type == PlaylistType.USER) 1 else 0,
        customImage: String? = null,
    ) = MigrationPlaylist(
        id = id,
        name = name,
        description = "",
        customImageFileName = customImage,
        sequence = sequence,
        type = type,
        createdTimeMillis = 0,
    )

    private fun track(playlistId: Int, audioMediaId: Int, sequence: Int) =
        MigrationTrack(playlistId, audioMediaId, sequence)
}
