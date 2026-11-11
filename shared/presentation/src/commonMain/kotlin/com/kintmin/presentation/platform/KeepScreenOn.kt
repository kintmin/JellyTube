package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/** [enabled] 인 동안 화면이 저절로 꺼지지 않게 한다. 화면을 떠나면 풀린다. */
@Composable
expect fun KeepScreenOnEffect(enabled: Boolean)
