package com.kintmin.domain.playlist.model

import com.kintmin.domain.audio_track.model.SecretTrack
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

data class Playlist(
    val id: Int,
    val name: String,
    val description: String,
    val audioMediaCount: Int,
    val playTimeDuration: Duration,
    val createdTime: LocalDateTime,
    val imageFileFullPath: String?,
    val isCustomImage: Boolean,
    val sequence: Int,
    val type: PlaylistType,
) {
    val isBasePlaylist: Boolean get() = type.isSystem

    /** "전체" 의 곡 수·길이는 저장된 누적값이라 숨긴 비밀 곡이 들어 있다. 그만큼 뺀다. */
    fun hidingSecretTracks(secretTracks: List<SecretTrack>): Playlist {
        if (type != PlaylistType.TOTAL || secretTracks.isEmpty()) return this
        return copy(
            audioMediaCount = audioMediaCount - secretTracks.size,
            playTimeDuration = playTimeDuration - secretTracks.fold(Duration.ZERO) { sum, it -> sum + it.playbackDuration },
        )
    }
}
