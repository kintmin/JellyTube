package com.kintmin.presentation.theme

import androidx.compose.ui.graphics.Color

val white = Color(0xFFFFFFFF)
val black = Color(0xFF000000)

val gray100 = Color(0xFFF5F5F5)
val gray80 = Color(0xFFD3D3D3)
val gray60 = Color(0xFFAEAEAE)
val gray40 = Color(0xFF838383)
val gray20 = Color(0xFF505050)
val gray10 = Color(0xFF2F2F2F)
val gray5 = Color(0xFF191919)

val dark100 = Color(0xFF040404)
val dark80 = Color(0xFF141414)
val dark60 = Color(0xFF212121)

/**
 * 재생 화면 위에 뜨는 창의 바탕. **뒤가 비쳐야** 아트워크에서 뽑은 그라데이션과 한 몸으로 보인다.
 * 불투명한 `surface` 를 쓰면 그 위에 납작한 회색 판이 얹힌다.
 */
val playerOverlay = Color(0xD10D1126)

// https://www.schemecolor.com/deep-sea-blue.php
val deepSea100 = Color(0xFF0D1126)
val deepSea80 = Color(0xFF1A224C)
val deepSea60 = Color(0xFF263373)
val deepSea40 = Color(0xFF31439B)

// https://www.schemecolor.com/bloody-reds.php
// 어두운 면 위의 파괴적 글자용. bloodyRed 계열은 밝은 테마용이라 가장 밝은 100 조차
// 어두운 면에서 본문 대비 바닥값에 못 미친다(3.42:1). 순수 빨강도 4.03:1 로 모자란다.
val softRed = Color(0xFFFF8A80)

val bloodyRed100 = Color(0xFFE90000)
val bloodyRed80 = Color(0xFFA40D03)
val bloodyRed60 = Color(0xFF7D0000)
val bloodyRed40 = Color(0xFF5A0607)

// https://www.schemecolor.com/blue-green-pastel-sea.php
val seaBlue100 = Color(0xFFACE5EE)
val seaBlue80 = Color(0xFF8ACCDC)
val seaBlue60 = Color(0xFF67B3C9)
val seaBlue40 = Color(0xFF459BB7)
val seaBlue20 = Color(0xFF2282A4)
val seaBlue10 = Color(0xFF006992)