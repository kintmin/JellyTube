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
 */
@Composable
expect fun PlatformFullScreenDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
)
