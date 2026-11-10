package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import platform.Foundation.NSBundle

@Composable
actual fun rememberAppVersionName(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
