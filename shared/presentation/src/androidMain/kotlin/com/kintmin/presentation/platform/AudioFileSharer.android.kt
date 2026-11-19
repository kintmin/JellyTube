package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Android 는 공용 저장소 복사 흐름(`DeviceSaveFlow.MediaStore`)이라 이 경로가 불리지 않는다. */
@Composable
actual fun rememberAudioFileSharer(): AudioFileSharer = remember { AudioFileSharer { _, _, _ -> false } }
