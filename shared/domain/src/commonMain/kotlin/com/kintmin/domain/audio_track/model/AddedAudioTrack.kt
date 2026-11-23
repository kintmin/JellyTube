package com.kintmin.domain.audio_track.model

/** 담기의 결과. [unlinkedAudioMediaCount] 는 비밀 플레이리스트에 담느라 다른 곳에서 빠진 곡 수다. */
data class AddedAudioTrack(
    val unlinkedAudioMediaCount: Int,
)
