package com.kintmin.domain.audio_media.model

/**
 * 여러 음원을 기기 공용 저장소로 내보낸 집계 결과.
 *
 * 내보내기는 곡 단위로 실패할 수 있고 일부 성공이 정상 결과라서, 실패를 예외로 던지지 않고
 * 여기에 모아 돌려준다.
 */
data class AudioMediaExportSummary(
    val exportedCount: Int,
    val alreadyExportedCount: Int,
    val failedNameList: List<String>,
) {
    val failedCount: Int get() = failedNameList.size
}
