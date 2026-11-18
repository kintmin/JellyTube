package com.kintmin.domain.audio_edit.usecase

import com.kintmin.domain.audio_edit.repository.AudioEditRepository

/**
 * 화면이 건 예약도 여기서 풀린다. 예약이 있어도 다시 거는 것은 예약 없이 도는 저장
 * (프로세스가 죽은 뒤 작업이 다시 도는 등)도 표시해야 해서다.
 */
internal inline fun <T> AudioEditRepository.withSavingMark(audioMediaId: Int, block: () -> T): T {
    reserveSaving(audioMediaId)
    try {
        return block()
    } finally {
        releaseSaving(audioMediaId)
    }
}
