package com.kintmin.domain.audio_track.model

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.playlist.model.Playlist

data class PlaylistTrackAggregate(
    val audioMedia: AudioMedia,
    val playlist: Playlist,
    val audioTrack: AudioTrack,
)