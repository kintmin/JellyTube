package com.kintmin.presentation.port.model

/** 데이터 이전이나 여러 곡 받기를 시작하기 전에 끝나 있어야 하는 작업. */
enum class RunningTask(val label: String) {
    YOUTUBE_DOWNLOAD("음원 다운로드"),
    LYRICS_VARIANT("가사 번역·음차"),
    ANOMALY_DATA_CHECK("이상 데이터 점검"),
    AUDIO_EDIT_SAVE("음원 편집 저장"),
    PC_FILE_SHARE("PC 연결"),
    BACKUP_RESTORE("백업 복원"),
    PLAYLIST_DOWNLOAD("재생목록 받기"),
}
