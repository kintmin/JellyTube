package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePicker(
    onPicked: (imageData: ByteArray) -> Unit,
): FilePickerLauncher
