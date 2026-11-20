package com.kintmin.presentation.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// M3 기본값은 64dp 다. 하단 네비게이션과 같은 높이인데 상단에만 상태바 inset 이 더해져 더
// 두껍게 보이므로 낮춰 쓴다.
val topAppBarHeight = 56.dp

val topAppBarCompactHeight = 48.dp

// M3 기본은 24dp 지만 이 앱은 아이콘을 22dp 로 쓴다.
// IconButton 이 48dp 터치 타깃을 따로 유지하므로 아이콘만 줄어도 누르는 영역은 그대로다.
val topAppBarIconSize = 22.dp

// 뒤로가기가 없는 루트 화면은 Root, 뒤로가기로 들어온 화면은 Detail 이다.
//
// 상단바는 높이가 고정이고 제목을 수직 중앙에 놓으므로, 글꼴 배율을 키웠을 때 라인이 바보다
// 높아지면 글자 상하가 깎인다.
val topAppBarRootTitleFontSize = 18.sp
val topAppBarRootTitleLineHeight = 22.sp

val topAppBarDetailTitleFontSize = 16.sp
val topAppBarDetailTitleLineHeight = 20.sp
