package com.kintmin.presentation.ui.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable

/**
 * 키보드가 올라와도 본문이 그 뒤로 들어가지 않는 Scaffold 의 contentWindowInsets.
 *
 * 본문에 `imePadding` 을 덧대면 네비바 높이가 두 번 들어가므로 이 자리에서 union 으로 먹는다.
 * **bottomBar 가 있는 화면에는 쓸 수 없다** — Scaffold 는 bottomBar 가 있으면 이 값 대신 그
 * 높이를 본문 여백으로 쓴다. 그런 화면은 스크롤 컨테이너에 직접 `imePadding` 을 준다.
 */
@OptIn(ExperimentalMaterial3Api::class)
val imeAwareContentWindowInsets: WindowInsets
    @Composable get() = ScaffoldDefaults.contentWindowInsets.union(WindowInsets.ime)
