package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/**
 * 화면 전체를 덮는 다이얼로그 컨테이너. 스크림과 내용은 호출자가 그린다.
 *
 * [hideSystemBars] 는 떠 있는 동안 상태바·내비게이션바를 숨기고 사라질 때 되돌린다.
 */
@Composable
expect fun PlatformFullScreenDialog(
    onDismissRequest: () -> Unit,
    hideSystemBars: Boolean = false,
    content: @Composable () -> Unit,
)

/**
 * 바텀시트가 떠 있는 동안 내비게이션바 아이콘 밝기를 고정한다.
 *
 * `ModalBottomSheet` 는 자기 창을 가져 액티비티 설정이 적용되지 않는다. iOS 는 no-op.
 */
@Composable
expect fun PlatformBottomSheetSystemBarEffect(lightNavigationBarIcons: Boolean)
