package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/**
 * 화면 전체를 덮는 다이얼로그 컨테이너.
 *
 * 스크림과 내용은 호출자가 그린다. 이 함수는 "다이얼로그 창이 화면 전체를 차지하고
 * 시스템 기본 배경·여백을 쓰지 않는다"는 것만 보장한다.
 *
 * Android 는 `DialogProperties.decorFitsSystemWindows` 와 창 배경·DIM 플래그를
 * 직접 만져야 해서 공통 `Dialog` 로 대체할 수 없다. 그 차이를 여기서 흡수한다.
 *
 * [hideSystemBars] 가 true 면 다이얼로그가 떠 있는 동안 상태바·내비게이션바를 숨기고
 * 사라질 때 되돌린다. 전체화면 이미지 뷰어처럼 화면을 독점하는 경우에 쓴다.
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
 * `ModalBottomSheet` 는 자기 창을 가지므로 액티비티에 걸어둔 설정이 적용되지 않는다.
 * Android 전용 문제이고 iOS 에는 대응 개념이 없어 no-op 이다.
 */
@Composable
expect fun PlatformBottomSheetSystemBarEffect(lightNavigationBarIcons: Boolean)
