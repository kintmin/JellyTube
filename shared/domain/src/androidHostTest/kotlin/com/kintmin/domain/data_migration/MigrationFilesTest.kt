package com.kintmin.domain.data_migration

import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.playlist.model.PlaylistType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationFilesTest {

    private val audio = MigrationAudioMedia(
        id = 1, source = "s", name = "곡", artist = "", description = "", rawAudioDurationSeconds = 100,
        audioFileName = "a.m4a", imageFileName = "a.jpg", lyricFileName = "a.lrc", tjKaraokeNumber = null,
        editedAudioFileName = "e.m4a", editedAudioDurationSeconds = 90, createdTimeMillis = 0,
    )
    private val playlist = MigrationPlaylist(2, "A", "", "p.jpg", 1, PlaylistType.USER, 0)
    private val snapshot = MigrationSnapshot(listOf(audio), listOf(playlist), emptyList(), emptyList(), emptyList())

    @Test
    fun `음원 파일이 없는 곡은 뺀다`() {
        val result = MigrationFiles.keepAvailable(snapshot, available = setOf(MigrationFile(MigrationFileKind.IMAGE, "a.jpg")))

        assertTrue(result.audioMedias.isEmpty())
    }

    @Test
    fun `없는 부가 파일은 비우고 편집본이 없으면 편집 길이도 비운다`() {
        val result = MigrationFiles.keepAvailable(snapshot, available = setOf(MigrationFile(MigrationFileKind.AUDIO, "a.m4a")))

        val kept = result.audioMedias.single()
        assertNull(kept.imageFileName)
        assertNull(kept.lyricFileName)
        assertNull(kept.editedAudioFileName)
        assertNull(kept.editedAudioDurationSeconds)
        assertNull(result.playlists.single().customImageFileName)
    }

    @Test
    fun `파일이 다 있으면 그대로다`() {
        val all = MigrationFiles.referencedBy(snapshot.audioMedias, snapshot.playlists)

        assertEquals(snapshot, MigrationFiles.keepAvailable(snapshot, all))
    }

    @Test
    fun `디렉터리를 벗어날 수 있는 이름은 거부한다`() {
        assertTrue(MigrationFiles.isSafeName("0f1e-uuid.m4a"))
        listOf("", ".", "..", "../a.m4a", "a/b.m4a", "..\\a.m4a", "a\u0000.m4a").forEach { name ->
            assertFalse(name, MigrationFiles.isSafeName(name))
        }
    }
}
