package com.kintmin.data.local_file.model

data class ExportedAudioInfo(
    /** 공용 저장소에 확정된 파일명(확장자 포함). 동명이곡 회피로 " (1)" 등이 붙을 수 있다. */
    val displayNameWithExt: String,

    /** 요청한 모든 용도에 같은 파일이 이미 있어 아무것도 복사하지 않은 경우 true */
    val alreadyExported: Boolean,
)
