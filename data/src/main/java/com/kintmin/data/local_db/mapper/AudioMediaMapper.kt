package com.kintmin.data.local_db.mapper

import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_db.model.PlaylistTrackEntity
import com.kintmin.data.local_file.FileManager
import com.kintmin.domain.extension.toLocalDateTime
import com.kintmin.domain.model.AudioMedia
import com.kintmin.domain.model.Playlist
import kotlin.time.Duration.Companion.seconds

internal object AudioMediaMapper {
    /**
     * Result.failure: 확장자명이 잘못되거나 없음, 또는 기본 폴더가 없음
     */
    fun toDomain(
        fileManager: FileManager,
        audioMediaEntity: AudioMediaEntity,
        playlistTrackEntity: PlaylistTrackEntity? = null,
    ): Result<AudioMedia> = runCatching {
        val audioFileFullPath = fileManager.getFullPathWithExt(
            fileName = audioMediaEntity.source,
            extName = audioMediaEntity.audioFileExt,
        ).getOrThrow()

        val imageFileFullPath = audioMediaEntity.imageFileExt?.let { imageFileExtName ->
            fileManager.getFullPathWithExt(
                fileName = audioMediaEntity.source,
                extName = imageFileExtName,
            ).getOrNull()
        }

        AudioMedia(
            id = audioMediaEntity.id,
            playlistId = playlistTrackEntity?.playlistId ?: Playlist.TOTAL,
            audioMediaSequence = playlistTrackEntity?.sequence ?: 0,
            sourcePath = audioMediaEntity.source,
            mediaName = audioMediaEntity.mediaName,
            artist = audioMediaEntity.artist,
            description = audioMediaEntity.description,
            audioDuration = audioMediaEntity.rawAudioDurationSeconds?.seconds,
            createdTime = audioMediaEntity.rawCreatedTime.toLocalDateTime(),
            audioFileFullPath = audioFileFullPath,
            imageFileFullPath = imageFileFullPath,
        )
    }
}
