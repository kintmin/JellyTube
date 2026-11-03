package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS 파일 선택기는 `UIDocumentPickerViewController` 로 띄워야 한다.
 *
 * ComposeUIViewController 바깥의 UIKit 작업이라 iOS 화면 작업과 함께 다룬다.
 * 그때까지 아무 일도 하지 않는다 — 가짜 결과를 돌려주면 호출부가
 * "선택했는데 아무것도 안 들어왔다"로 오해한다.
 */
@Composable
actual fun rememberAudioFilePicker(
    startAtAudioRoot: Boolean,
    onPicked: (fileIdentifiers: List<String>) -> Unit,
): FilePickerLauncher = remember {
    FilePickerLauncher { println("[FilePicker] iOS 파일 선택기가 아직 없다") }
}
