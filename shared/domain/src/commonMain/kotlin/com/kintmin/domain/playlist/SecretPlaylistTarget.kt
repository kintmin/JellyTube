package com.kintmin.domain.playlist

import com.kintmin.domain.playlist.repository.PlaylistRepository

/** 받은 곡은 담는 플레이리스트를 따라 비밀이 된다. 대상이 없으면 미분류다. 확인하지 못하면 비밀로 둔다. */
suspend fun PlaylistRepository.isSecretTarget(playlistId: Int?): Boolean =
    playlistId != null && getPlaylist(playlistId).map { it.type.isSecret }.getOrDefault(true)
