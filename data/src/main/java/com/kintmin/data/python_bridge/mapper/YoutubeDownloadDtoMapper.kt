package com.kintmin.data.python_bridge.mapper

import com.kintmin.data.python_bridge.model.YoutubeDownloadDto
import com.kintmin.domain.extension.toLocalDateTime
import com.kintmin.domain.model.DownloadedAudioMedia
import java.time.Instant

internal fun YoutubeDownloadDto.toDomain(
    source: String,
    audioFileNameWithExt: String,
    imageFileNameWithExt: String?,
): DownloadedAudioMedia {
    return DownloadedAudioMedia(
        source = source,
        title = title,
        duration = duration.toLongOrNull(),
        uploader = uploader,
        description = description,
        createdTime = Instant.now().toLocalDateTime(),
        audioFileNameWithExt = audioFileNameWithExt,
        imageFileNameWithExt = imageFileNameWithExt,
    )
}