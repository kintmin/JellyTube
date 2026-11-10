package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

fun interface FilePickerLauncher {

    fun launch()
}

/**
 * 선택 결과는 플랫폼이 다루는 파일 식별자 문자열이다(Android 는 `content://` URI).
 *
 * [startAtAudioRoot] 는 선택기를 "오디오" 루트에서 연다. 대응물이 없는 플랫폼은 무시한다.
 */
@Composable
expect fun rememberAudioFilePicker(
    startAtAudioRoot: Boolean = false,
    onPicked: (fileIdentifiers: List<String>) -> Unit,
): FilePickerLauncher
