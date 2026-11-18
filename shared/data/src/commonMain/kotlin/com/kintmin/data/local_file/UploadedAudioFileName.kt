package com.kintmin.data.local_file

internal class UploadedAudioFileName(originalFileName: String) {
    val ext: String = originalFileName.substringAfterLast('.', "")

    /** 음원 메타데이터에 제목이 없을 때 쓴다. */
    val fallbackTitle: String = originalFileName.substringBeforeLast('.')

    init {
        require(ext.isNotBlank()) { "확장자가 없는 파일명입니다: $originalFileName" }
    }
}
