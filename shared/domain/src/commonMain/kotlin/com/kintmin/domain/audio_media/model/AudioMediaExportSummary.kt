package com.kintmin.domain.audio_media.model

data class AudioMediaExportSummary(
    val exportedCount: Int,
    val alreadyExportedCount: Int,
    val failedNameList: List<String>,
) {
    val failedCount: Int get() = failedNameList.size
}
