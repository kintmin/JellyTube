package com.kintmin.domain.audio_media.model

/** 음원 1개를 기기 공용 저장소로 내보낸 결과. */
enum class AudioMediaExportStatus {
    /** 이번 호출에서 공용 저장소에 복사됐다. */
    EXPORTED,

    /** 요청한 모든 용도에 같은 파일이 이미 있어 복사하지 않았다. */
    ALREADY_EXPORTED,
}
