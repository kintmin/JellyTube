package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/** 파일 선택기를 띄운다. */
fun interface FilePickerLauncher {

    fun launch()
}

/**
 * 오디오 파일 다중 선택기.
 *
 * 선택 결과는 플랫폼이 다루는 파일 식별자 문자열로 넘어온다.
 * Android 는 `content://` URI 문자열이고, 그대로 UseCase 에 전달된다.
 *
 * [startAtAudioRoot] 가 true 면 선택기를 "오디오" 루트에서 연다.
 * Android 에서만 의미가 있고 지원하지 않는 플랫폼은 무시한다.
 */
@Composable
expect fun rememberAudioFilePicker(
    startAtAudioRoot: Boolean = false,
    onPicked: (fileIdentifiers: List<String>) -> Unit,
): FilePickerLauncher
