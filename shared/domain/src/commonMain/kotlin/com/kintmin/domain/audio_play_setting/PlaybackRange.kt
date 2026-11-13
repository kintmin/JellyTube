package com.kintmin.domain.audio_play_setting

/** 재생 속도·피치로 고를 수 있는 범위. */
object PlaybackRange {
    const val MIN_SPEED = 0.25f
    const val MAX_SPEED = 3.0f
    const val MIN_PITCH_SEMITONE = -10
    const val MAX_PITCH_SEMITONE = 10

    fun clampSpeed(speed: Float): Float = speed.coerceIn(MIN_SPEED, MAX_SPEED)

    fun clampPitchSemitone(semitone: Int): Int = semitone.coerceIn(MIN_PITCH_SEMITONE, MAX_PITCH_SEMITONE)
}
