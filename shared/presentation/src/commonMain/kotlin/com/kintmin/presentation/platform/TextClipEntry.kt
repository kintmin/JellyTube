package com.kintmin.presentation.platform

import androidx.compose.ui.platform.ClipEntry

/** 일반 텍스트 [ClipEntry]. 생성자가 플랫폼마다 다르다. */
expect fun textClipEntry(text: String): ClipEntry
