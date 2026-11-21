package com.kintmin.presentation.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val topAppBarHeight = 56.dp

val topAppBarCompactHeight = 48.dp

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
