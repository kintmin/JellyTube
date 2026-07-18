package com.kintmin.data.local_db.mapper

import com.kintmin.data.local_db.model.PlaylistEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.extension.toLocalDateTime
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
import kotlin.time.Duration.Companion.seconds

internal fun PlaylistEntity.toDomain(fileManager: FileManager) = runCatching {
    Playlist(
        id = id,
        name = name,
        description = description,
        audioMediaCount = audioMediaCount,
        playTimeDuration = rawPlayTimeDuration.seconds,
        createdTime = rawCreatedTime.toLocalDateTime(),
        imageFileFullPath = imageFileNameWithExt?.let {
            fileManager.getImageFileFullPath(fileNameWithExt = it).getOrThrow()
        },
        isCustomImage = isCustomImage,
        sequence = sequence,
        type = runCatching { PlaylistType.valueOf(type) }.getOrDefault(PlaylistType.USER),
    )
}
