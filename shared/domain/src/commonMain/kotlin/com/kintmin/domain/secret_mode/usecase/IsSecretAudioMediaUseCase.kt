package com.kintmin.domain.secret_mode.usecase

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.secret_mode.secretAudioMediaJudge
import com.kintmin.log.AppLog

/** 곡 하나가 비밀인지 묻는다. 목록을 읽지 못하면 비밀로 본다 — 틀릴 때 덜 새는 쪽이다. */
class IsSecretAudioMediaUseCase constructor(
    private val audioTrackRepository: AudioTrackRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(audioMediaId: Int): Boolean =
        audioTrackRepository.secretAudioMediaJudge(appLog)(audioMediaId)
}
