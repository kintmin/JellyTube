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
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.MigrationAudioMediaDto
import com.kintmin.fileshare.MigrationDownloadSettingsDto
import com.kintmin.fileshare.MigrationEditProjectDto
import com.kintmin.fileshare.MigrationFileDto
import com.kintmin.fileshare.MigrationManifestDto
import com.kintmin.fileshare.MigrationPlaylistDto
import com.kintmin.fileshare.MigrationSnapshotDto
import com.kintmin.fileshare.MigrationStepDto
import com.kintmin.fileshare.MigrationTrackDto

fun MigrationExport.toManifestDto() = MigrationManifestDto(
    formatVersion = FileShareConstants.MIGRATION_FORMAT_VERSION,
    snapshot = MigrationSnapshotDto(
        audioMedias = snapshot.audioMedias.map { it.toDto() },
        playlists = snapshot.playlists.map { it.toDto() },
        tracks = snapshot.tracks.map { MigrationTrackDto(it.playlistId, it.audioMediaId, it.sequence) },
        editProjects = snapshot.editProjects.map { MigrationEditProjectDto(it.audioMediaId, it.timelineJson) },
        steps = snapshot.steps.map { MigrationStepDto(it.createdTimeMillis, it.stepSensor) },
    ),
    files = files.map { MigrationFileDto(kind = it.file.kind.name, name = it.file.name, size = it.size) },
    downloadSettings = downloadSettings?.let { MigrationDownloadSettingsDto(it.shouldInsertAtTop, it.playlistId) },
)

/** 모르는 종류의 파일은 버린다. 그 파일을 가리키는 값은 받지 못한 것으로 친다. */
fun MigrationManifestDto.toExport() = MigrationExport(
    snapshot = MigrationSnapshot(
        audioMedias = snapshot.audioMedias.map { it.toDomain() },
        playlists = snapshot.playlists.map { it.toDomain() },
        tracks = snapshot.tracks.map { MigrationTrack(it.playlistId, it.audioMediaId, it.sequence) },
        editProjects = snapshot.editProjects.map { MigrationEditProject(it.audioMediaId, it.timelineJson) },
        steps = snapshot.steps.map { MigrationStep(it.createdTimeMillis, it.stepSensor) },
    ),
    files = files.mapNotNull { dto ->
        val kind = runCatching { MigrationFileKind.valueOf(dto.kind) }.getOrNull() ?: return@mapNotNull null
        MigrationFileEntry(MigrationFile(kind, dto.name), dto.size)
    },
    downloadSettings = downloadSettings?.let { MigrationDownloadSettings(it.shouldInsertAtTop, it.playlistId) },
)

private fun MigrationAudioMedia.toDto() = MigrationAudioMediaDto(
    id = id,
    source = source,
    name = name,
    artist = artist,
    description = description,
    rawAudioDurationSeconds = rawAudioDurationSeconds,
    audioFileName = audioFileName,
    imageFileName = imageFileName,
    lyricFileName = lyricFileName,
    tjKaraokeNumber = tjKaraokeNumber,
    editedAudioFileName = editedAudioFileName,
    editedAudioDurationSeconds = editedAudioDurationSeconds,
    createdTimeMillis = createdTimeMillis,
)

private fun MigrationAudioMediaDto.toDomain() = MigrationAudioMedia(
    id = id,
    source = source,
    name = name,
    artist = artist,
    description = description,
    rawAudioDurationSeconds = rawAudioDurationSeconds,
    audioFileName = audioFileName,
    imageFileName = imageFileName,
    lyricFileName = lyricFileName,
    tjKaraokeNumber = tjKaraokeNumber,
    editedAudioFileName = editedAudioFileName,
    editedAudioDurationSeconds = editedAudioDurationSeconds,
    createdTimeMillis = createdTimeMillis,
)

private fun MigrationPlaylist.toDto() = MigrationPlaylistDto(
    id = id,
    name = name,
    description = description,
    customImageFileName = customImageFileName,
    sequence = sequence,
    type = type.name,
    createdTimeMillis = createdTimeMillis,
)

private fun MigrationPlaylistDto.toDomain() = MigrationPlaylist(
    id = id,
    name = name,
    description = description,
    customImageFileName = customImageFileName,
    sequence = sequence,
    type = runCatching { PlaylistType.valueOf(type) }.getOrDefault(PlaylistType.USER),
    createdTimeMillis = createdTimeMillis,
)
