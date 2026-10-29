package com.kintmin.domain.audio_media.model

/**
 * 음원을 기기 공용 저장소로 내보낼 때의 용도.
 *
 * MediaStore의 is_music/is_alarm/is_notification/is_ringtone 컬럼은 readOnly라서 ContentValues로
 * 넣을 수 없다(넣어도 MediaProvider가 버린다). MediaProvider는 이 값들을 파일이 놓인 공용
 * 디렉토리 경로에서 재계산하므로, "어떤 목록(알람음/알림음/벨소리/음악)에 노출되는지"는 복사
 * 대상 디렉토리로만 결정된다 — 즉 용도 하나가 복사본 하나다.
 */
enum class AudioExportTarget {
    MUSIC,
    ALARM,
    NOTIFICATION,
    RINGTONE,
}
