package com.kintmin.presentation.platform

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual fun textClipEntry(text: String): ClipEntry = ClipEntry(ClipData.newPlainText(text, text))
