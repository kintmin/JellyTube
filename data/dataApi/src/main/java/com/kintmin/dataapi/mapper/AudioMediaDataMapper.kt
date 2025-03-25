package com.kintmin.dataapi.mapper

import com.kintmin.domain.model.AudioMediaData
import com.kintmin.localdatabase.entity.AudioMediaEntity
import com.kintmin.localfile.FileManager
import com.kintmin.pythonbridge.model.YoutubeDownloadDto
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal fun AudioMediaData.toEntity(fileManager: FileManager): AudioMediaEntity {
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

internal fun AudioMediaEntity.toDomain(fileManager: FileManager): Result<AudioMediaData> = runCatching {
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

    AudioMediaData(
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
): AudioMediaData {
    return AudioMediaData(
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