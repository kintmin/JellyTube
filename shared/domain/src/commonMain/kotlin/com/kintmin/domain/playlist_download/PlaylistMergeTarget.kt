package com.kintmin.domain.playlist_download

import com.kintmin.domain.playlist.PlaylistTextLimit
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType

/**
 * 저장되는 이름은 상한으로 잘리므로 양쪽을 같은 상한에 통과시켜 비교한다. 원제로 비교하면 다시 받을
 * 때마다 같은 재생목록이 새로 생기고, 상한을 넘겨 이미 저장된 이름은 왼쪽도 통과시켜야 합쳐진다.
 */
fun List<Playlist>.findYoutubeMergeTarget(youtubeTitle: String): Playlist? {
    val name = PlaylistTextLimit.clampTitle(youtubeTitle)
    return firstOrNull { it.type == PlaylistType.USER && PlaylistTextLimit.clampTitle(it.name) == name }
}
