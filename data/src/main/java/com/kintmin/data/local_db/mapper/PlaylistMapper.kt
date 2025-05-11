package com.kintmin.data.local_db.mapper

import com.kintmin.data.local_db.model.PlaylistEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.common.extension.toLocalDateTime
import com.kintmin.domain.playlist.model.Playlist
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
            fileManager.getFullPathWithExt(fileNameWithExt = it).getOrThrow()
        },
        isCustomImage = isCustomImage,
    )
}
