package com.kintmin.domain.secret_mode

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import kotlinx.coroutines.flow.first

/**
 * 받은 곡은 담는 플레이리스트를 따라 비밀이 된다. 대상이 없으면 미분류다. 확인하지 못하면 비밀로 둔다.
 * 지워진 플레이리스트도 확인 실패라 받기는 미분류로 가므로 디버그로만 남긴다.
 */
suspend fun PlaylistRepository.isSecretTarget(playlistId: Int?, appLog: AppLog): Boolean {
    if (playlistId == null) return false
    return getPlaylist(playlistId).map { it.type.isSecret }
        .onFailure { appLog.sendDebugLog(DebugLog(LOG_TAG, "담을 플레이리스트 확인 실패라 비밀로 봄: id=$playlistId ${it::class.simpleName}")) }
        .getOrDefault(true)
}

/** 곡 id 가 비밀 곡인지 판정한다. 목록을 읽지 못하면 모두 비밀로 본다. */
suspend fun AudioTrackRepository.secretAudioMediaJudge(appLog: AppLog): (Int) -> Boolean {
    val secretIds = suspendRunCatching { getSecretAudioMediaIdListFlow().first().toSet() }
        .onFailure { appLog.sendErrorLog(ErrorLog(LOG_TAG, "비밀 곡 목록 읽기 실패라 모두 비밀로 봄", it)) }
        .getOrNull()
        ?: return { true }
    return { it in secretIds }
}

private const val LOG_TAG = "SecretDataJudge"
