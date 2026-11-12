package com.kintmin.presentation.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.kintmin.presentation.platform.PlatformBottomSheetSystemBarEffect

/**
 * 앱의 모든 바텀시트가 쓰는 컨테이너. 시트 높이는 내용 높이를 따르고, 화면을 넘으면 내용이 스크롤된다.
 *
 * 중간 높이(partially expanded)에 멈추지 않는다 — 멈추면 같은 시트가 내용 길이에 따라 반만 뜨기도 해서
 * 시트마다 동작이 달라 보인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JellyTubeBottomSheet(
    onDismissRequest: () -> Unit,
    sheetGesturesEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = sheetGesturesEnabled,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        PlatformBottomSheetSystemBarEffect(lightNavigationBarIcons = false)
        content()
    }
}
