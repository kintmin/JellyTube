package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/** 사진 한 장을 골라 그 바이트를 넘긴다. 취소하면 아무것도 넘기지 않는다. */
@Composable
expect fun rememberImagePicker(
    onPicked: (imageData: ByteArray) -> Unit,
): FilePickerLauncher
