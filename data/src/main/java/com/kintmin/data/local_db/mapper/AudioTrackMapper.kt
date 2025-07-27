package com.kintmin.data.local_db.mapper

import com.kintmin.data.local_db.model.PlaylistTrackEntity
import com.kintmin.data.local_db.model.PlaylistTrackFullDto
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.audio_track.model.AudioTrack
import com.kintmin.domain.extension.toLocalDateTime
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate

internal fun PlaylistTrackFullDto.toDomain(fileManager: FileManager) = runCatching {
    PlaylistTrackAggregate(
        audioMedia = audioMediaEntity.toDomain(fileManager).getOrThrow(),
        playlist = playlistEntity.toDomain(fileManager).getOrThrow(),
        audioTrack = playlistTrackEntity.toDomain(),
    )
}

internal fun PlaylistTrackEntity.toDomain() = AudioTrack(
    audioMediaId = audioMediaId,
    playlistId = playlistId,
    trackSequence = sequence,
    trackAddedTime = rawCreatedTime.toLocalDateTime(),
)