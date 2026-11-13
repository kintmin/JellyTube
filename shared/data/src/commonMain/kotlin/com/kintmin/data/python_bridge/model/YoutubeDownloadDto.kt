package com.kintmin.data.python_bridge.model

data class YoutubeDownloadDto(
    val title: String,
    val thumbnailDownloadUrl: String,
    val duration: String,
    val uploader: String,
    val description: String,
    val audioFileNameWithExt: String,
)

/** 추출기가 비워 준 정보는 화면에 보일 기본값으로 채운다. */
fun YoutubeDownloadDto.withDefaults(): YoutubeDownloadDto = copy(
    title = title.ifBlank { UNKNOWN_TEXT },
    duration = duration.ifBlank { UNKNOWN_DURATION },
    uploader = uploader.ifBlank { UNKNOWN_TEXT },
)

private const val UNKNOWN_TEXT = "알 수 없음"
private const val UNKNOWN_DURATION = "0"