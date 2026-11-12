package com.kintmin.fileshare

import kotlinx.serialization.Serializable

@Serializable
data class MigrationPairRequest(
    val code: String,
    /** 보내는 기기 화면에 연결된 상대로 보여 준다. */
    val deviceName: String,
)

@Serializable
data class MigrationPairResponse(
    val token: String,
)

@Serializable
data class MigrationFinishRequest(
    val newAudioMediaCount: Int,
    val failedCount: Int,
)

@Serializable
data class MigrationManifestDto(
    val formatVersion: Int,
    val snapshot: MigrationSnapshotDto,
    val files: List<MigrationFileDto>,
    val downloadSettings: MigrationDownloadSettingsDto? = null,
)

@Serializable
data class MigrationDownloadSettingsDto(
    val shouldInsertAtTop: Boolean,
    val playlistId: Int? = null,
)

@Serializable
data class MigrationSnapshotDto(
    val audioMedias: List<MigrationAudioMediaDto>,
    val playlists: List<MigrationPlaylistDto>,
    val tracks: List<MigrationTrackDto>,
    val editProjects: List<MigrationEditProjectDto>,
    val steps: List<MigrationStepDto>,
)

@Serializable
data class MigrationAudioMediaDto(
    val id: Int,
    val source: String,
    val name: String,
    val artist: String,
    val description: String,
    val rawAudioDurationSeconds: Long? = null,
    val audioFileName: String,
    val imageFileName: String? = null,
    val lyricFileName: String? = null,
    val tjKaraokeNumber: String? = null,
    val editedAudioFileName: String? = null,
    val editedAudioDurationSeconds: Long? = null,
    val createdTimeMillis: Long,
)

@Serializable
data class MigrationPlaylistDto(
    val id: Int,
    val name: String,
    val description: String,
    val customImageFileName: String? = null,
    val sequence: Int,
    /** `PlaylistType` 의 name. */
    val type: String,
    val createdTimeMillis: Long,
)

@Serializable
data class MigrationTrackDto(
    val playlistId: Int,
    val audioMediaId: Int,
    val sequence: Int,
)

@Serializable
data class MigrationEditProjectDto(
    val audioMediaId: Int,
    val timelineJson: String,
)

@Serializable
data class MigrationStepDto(
    val createdTimeMillis: Long,
    val stepSensor: Long,
)

@Serializable
data class MigrationFileDto(
    /** `MigrationFileKind` 의 name. 경로에도 이 값을 쓴다. */
    val kind: String,
    val name: String,
    val size: Long,
)
