package com.kintmin.data.local_db.mapper

import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.extension.toLocalDateTime
import kotlin.time.Duration.Companion.seconds

internal fun AudioMediaEntity.toDomain(fileManager: FileManager) = runCatching {
    AudioMedia(
        id = id,
        source = source,
        name = name,
        artist = artist,
        description = description,
        audioDuration = rawAudioDurationSeconds?.seconds,
        audioFileFullPath = fileManager.getAudioFileFullPath(fileNameWithExt = audioFileNameWithExt).getOrThrow(),
        imageFileFullPath = imageFileNameWithExt?.let {
            fileManager.getImageFileFullPath(fileNameWithExt = it).getOrThrow()
        },
        lyricFileFullPath = lyricFileNameWithExt?.let {
            fileManager.getLyricFileFullPath(fileNameWithExt = it).getOrThrow()
        },
        tjKaraokeNumber = tjKaraokeNumber,
        createdTime = rawCreatedTime.toLocalDateTime(),
    )
}
