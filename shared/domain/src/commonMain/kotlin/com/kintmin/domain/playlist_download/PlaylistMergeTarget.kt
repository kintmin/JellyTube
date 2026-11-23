package com.kintmin.domain.playlist_download

import com.kintmin.domain.playlist.PlaylistTextLimit
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType

/**
 * 유튜브 제목으로 합칠 재생목록을 찾는다. 저장되는 이름은 상한으로 잘리므로 **양쪽을 같은
 * 상한에 통과시켜** 비교한다 — 원제로 비교하면 다시 받을 때마다 매칭이 깨져 같은 재생목록이
 * 매번 새로 생긴다. 이미 저장된 상한 초과 이름도 왼쪽을 통과시켜야 계속 합쳐진다.
 *
 * 미리보기와 실제 받기가 **같은 답**을 내야 해서 한 자리에 둔다.
 */
fun List<Playlist>.findYoutubeMergeTarget(youtubeTitle: String): Playlist? {
    val name = PlaylistTextLimit.clampTitle(youtubeTitle)
    return firstOrNull { it.type == PlaylistType.USER && PlaylistTextLimit.clampTitle(it.name) == name }
}
