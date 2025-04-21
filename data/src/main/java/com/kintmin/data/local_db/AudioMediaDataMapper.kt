package com.kintmin.data.local_db

import com.kintmin.data.local_db.entity.AudioMediaEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.python_bridge.model.YoutubeDownloadDto
import com.kintmin.domain.model.AudioMedia
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds

internal fun AudioMedia.toEntity(fileManager: FileManager): AudioMediaEntity {
    val zoneOffset = ZoneId.systemDefault().rules.getOffset(createdTime)
    val audioFileNameWithExt = fileManager.getFileNameWithExt(audioFileFullPath).getOrThrow()
    val imageFileNameWithExt = imageFileFullPath?.let {
        fileManager.getFileNameWithExt(it).getOrNull()
    }

    return AudioMediaEntity(
        id = id,
        playlistId = playlistId,
        artist = artist,
        mediaName = mediaName,
        description = description,
        rawAudioDuration = audioDuration?.inWholeSeconds,
        rawCreatedTime = createdTime.toEpochSecond(zoneOffset),
        audioFileNameWithExt = audioFileNameWithExt,
        imageFileNameWithExt = imageFileNameWithExt,
    )
}

internal fun AudioMediaEntity.toDomain(fileManager: FileManager): Result<AudioMedia> = runCatching {
    val audioFilePath = fileManager.getFullPathWithExt(
        fileNameWithExt = audioFileNameWithExt
    ).getOrThrow()

    val imageFilePath = imageFileNameWithExt?.let { imageFileNameWithExt ->
        fileManager.getFullPathWithExt(
            fileNameWithExt = imageFileNameWithExt
        ).getOrNull()
    }

    val createdTime = Instant.ofEpochMilli(rawCreatedTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()

    AudioMedia(
        id = id,
        playlistId = playlistId,
        mediaName = mediaName,
        description = description,
        artist = artist,
        audioDuration = rawAudioDuration?.seconds,
        createdTime = createdTime,
        audioFileFullPath = audioFilePath,
        imageFileFullPath = imageFilePath,
    )
}


internal fun YoutubeDownloadDto.toDomain(
    id: String,
    createdTime: LocalDateTime,
    audioFileFullPath: String,
    imageFileFullPath: String?,
): AudioMedia {
    return AudioMedia(
        id = id,
        playlistId = null,
        mediaName = title,
        description = description,
        artist = uploader,
        audioDuration = duration.toLongOrNull()?.seconds,
        createdTime = createdTime,
        audioFileFullPath = audioFileFullPath,
        imageFileFullPath = imageFileFullPath,
    )
}