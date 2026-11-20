package com.kintmin.presentation.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// M3 기본값은 64dp 다. 하단 네비게이션과 같은 높이인데 상단에만 상태바 inset 이 더해져 더
// 두껍게 보이므로 낮춰 쓴다. 모든 화면이 같은 값을 써야 화면을 옮길 때 상단바가 튀지 않는다.
val topAppBarHeight = 56.dp

// M3 기본은 24dp 지만 이 앱은 아이콘을 22dp 로 쓴다(하단 탭·PlayerBar·설정 항목).
// IconButton 이 48dp 터치 타깃을 따로 유지하므로 아이콘만 줄어도 누르는 영역은 그대로다.
val topAppBarIconSize = 22.dp

// 제목 크기는 네비게이션 깊이로 가른다 — 뒤로가기가 없는 루트 화면은 Root, 뒤로가기로 들어온
// 화면은 Detail 이다. 그래야 규칙이 화면에서 읽힌다.
//
// lineHeight 가 topAppBarHeight 의 절반을 넘지 않아야 한다. 상단바는 높이가 고정이고 제목을
// 수직 중앙에 놓으므로, 글꼴 배율을 키웠을 때 라인이 바보다 높아지면 글자 상하가 깎인다.
// titleLarge 의 22sp/28sp 를 그대로 쓰면 배율 2.0 에서 라인이 바 높이와 같아져 여유가 0 이 된다.
val topAppBarRootTitleFontSize = 18.sp
val topAppBarRootTitleLineHeight = 22.sp

val topAppBarDetailTitleFontSize = 16.sp
val topAppBarDetailTitleLineHeight = 20.sp
