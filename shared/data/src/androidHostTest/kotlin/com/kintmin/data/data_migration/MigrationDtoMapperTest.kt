package com.kintmin.data.data_migration

import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationEditProject
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationStep
import com.kintmin.domain.data_migration.model.MigrationTrack
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.fileshare.MigrationFileDto
import com.kintmin.fileshare.MigrationManifestDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class MigrationDtoMapperTest {

    private val export = MigrationExport(
        snapshot = MigrationSnapshot(
            audioMedias = listOf(
                MigrationAudioMedia(
                    id = 1, source = "https://youtu.be/a", name = "곡", artist = "가수", description = "설명",
                    rawAudioDurationSeconds = 100, audioFileName = "a.m4a", imageFileName = "a.jpg",
                    lyricFileName = "a.lrc", tjKaraokeNumber = "123", editedAudioFileName = "e.m4a",
                    editedAudioDurationSeconds = 90, createdTimeMillis = 5,
                ),
            ),
            playlists = listOf(
                MigrationPlaylist(2, "드라이브", "", "p.jpg", 1, PlaylistType.USER, 6),
                MigrationPlaylist(3, "즐겨찾기", "", null, 0, PlaylistType.FAVORITE, 7),
            ),
            tracks = listOf(MigrationTrack(2, 1, 1)),
            editProjects = listOf(MigrationEditProject(1, "{}")),
            steps = listOf(MigrationStep(8, 9)),
        ),
        files = listOf(MigrationFileEntry(MigrationFile(MigrationFileKind.AUDIO, "a.m4a"), 10)),
        downloadSettings = MigrationDownloadSettings(shouldInsertAtTop = true, playlistId = 2),
    )

    @Test
    fun `JSON 을 거쳐 왕복해도 같은 값이다`() {
        val json = Json.encodeToString(MigrationManifestDto.serializer(), export.toManifestDto())

        assertEquals(export, Json.decodeFromString(MigrationManifestDto.serializer(), json).toExport())
    }

    @Test
    fun `설정이 없는 manifest 는 설정 없음으로 읽는다`() {
        val json = Json.encodeToString(MigrationManifestDto.serializer(), export.copy(downloadSettings = null).toManifestDto())

        assertEquals(null, Json.decodeFromString(MigrationManifestDto.serializer(), json).toExport().downloadSettings)
    }

    @Test
    fun `모르는 종류의 파일은 버린다`() {
        val dto = export.toManifestDto().copy(files = listOf(MigrationFileDto("VIDEO", "v.mp4", 1)))

        assertEquals(emptyList<MigrationFileEntry>(), dto.toExport().files)
    }
}
