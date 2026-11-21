package com.kintmin.data.python_bridge.model

import com.kintmin.domain.youtube.model.YoutubePlaylist
import com.kintmin.domain.youtube.model.YoutubePlaylistEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class YoutubePlaylistDto(
    val title: String = "",
    val entries: List<YoutubePlaylistEntryDto> = emptyList(),
)

@Serializable
internal data class YoutubePlaylistEntryDto(
    val id: String,
    val title: String = "",
    // yt-dlp 는 길이를 소수로 줄 때가 있다.
    val duration: Double? = null,
    val available: Boolean = true,
)

private val playlistJson = Json { ignoreUnknownKeys = true }

internal fun parseYoutubePlaylist(json: String): YoutubePlaylist {
    val dto = playlistJson.decodeFromString(YoutubePlaylistDto.serializer(), json)
    return YoutubePlaylist(
        title = dto.title,
        entries = dto.entries.map {
            YoutubePlaylistEntry(
                videoId = it.id,
                title = it.title,
                durationSeconds = it.duration?.toLong(),
                isAvailable = it.available,
            )
        },
    )
}
