package com.kintmin.data.local_db.mapper

import com.kintmin.data.local_db.model.AudioEditProjectEntity
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_db.model.PlaylistEntity
import com.kintmin.data.local_db.model.PlaylistTrackEntity
import com.kintmin.data.local_db.model.StepEntity
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationEditProject
import com.kintmin.domain.data_migration.model.MigrationPlaylist
import com.kintmin.domain.data_migration.model.MigrationStep
import com.kintmin.domain.data_migration.model.MigrationTrack
import com.kintmin.domain.playlist.model.PlaylistType

internal fun AudioMediaEntity.toMigration() = MigrationAudioMedia(
    id = id,
    source = source,
    name = name,
    artist = artist,
    description = description,
    rawAudioDurationSeconds = rawAudioDurationSeconds,
    audioFileName = audioFileNameWithExt,
    imageFileName = imageFileNameWithExt,
    lyricFileName = lyricFileNameWithExt,
    tjKaraokeNumber = tjKaraokeNumber,
    editedAudioFileName = editedAudioFileNameWithExt,
    editedAudioDurationSeconds = editedAudioDurationSeconds,
    createdTimeMillis = rawCreatedTime,
)

internal fun PlaylistEntity.toMigration() = MigrationPlaylist(
    id = id,
    name = name,
    description = description,
    customImageFileName = imageFileNameWithExt.takeIf { isCustomImage },
    sequence = sequence,
    type = runCatching { PlaylistType.valueOf(type) }.getOrDefault(PlaylistType.USER),
    createdTimeMillis = rawCreatedTime,
)

internal fun PlaylistTrackEntity.toMigration() = MigrationTrack(
    playlistId = playlistId,
    audioMediaId = audioMediaId,
    sequence = sequence,
)

internal fun AudioEditProjectEntity.toMigration() = MigrationEditProject(
    audioMediaId = audioMediaId,
    timelineJson = timelineJson,
)

internal fun StepEntity.toMigration() = MigrationStep(
    createdTimeMillis = rawCreatedTime,
    stepSensor = stepSensor,
)
