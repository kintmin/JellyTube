package com.kintmin.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

// 팔레트는 이 파일 밖으로 내보내지 않는다. 화면은 아래 의미 토큰이나 MaterialTheme.colorScheme 만 쓴다.

private val white = Color(0xFFFFFFFF)
private val black = Color(0xFF000000)

private val gray100 = Color(0xFFF5F5F5)
private val gray80 = Color(0xFFD3D3D3)
private val gray60 = Color(0xFFAEAEAE)
private val gray40 = Color(0xFF838383)
private val gray20 = Color(0xFF505050)
private val gray10 = Color(0xFF2F2F2F)

private val dark100 = Color(0xFF040404)
private val dark80 = Color(0xFF141414)
private val dark60 = Color(0xFF212121)

// https://www.schemecolor.com/deep-sea-blue.php
private val deepSea100 = Color(0xFF0D1126)
private val deepSea80 = Color(0xFF1A224C)
private val deepSea40 = Color(0xFF31439B)

// 어두운 면 위의 파괴적 글자용. bloodyRed 계열은 밝은 테마용이라 가장 밝은 100 조차
// 어두운 면에서 본문 대비 바닥값에 못 미친다(3.42:1). 순수 빨강도 4.03:1 로 모자란다.
private val softRed = Color(0xFFFF8A80)

// https://www.schemecolor.com/bloody-reds.php
private val bloodyRed100 = Color(0xFFE90000)
private val bloodyRed80 = Color(0xFFA40D03)
private val bloodyRed40 = Color(0xFF5A0607)

// https://www.schemecolor.com/blue-green-pastel-sea.php
private val seaBlue100 = Color(0xFFACE5EE)
private val seaBlue60 = Color(0xFF67B3C9)
private val seaBlue40 = Color(0xFF459BB7)
private val seaBlue20 = Color(0xFF2282A4)
private val seaBlue10 = Color(0xFF006992)

internal val DarkColorScheme = darkColorScheme(
    primary = seaBlue10,
    onPrimary = white,
    primaryContainer = deepSea80,
    onPrimaryContainer = gray100,

    secondary = deepSea80,
    onSecondary = gray100,
    secondaryContainer = deepSea80,
    onSecondaryContainer = gray100,

    // 켜짐·재생 중 강조. primary 는 어두운 배경과 scrim 위에서 꺼짐보다 어둡게 보인다.
    tertiary = seaBlue60,
    onTertiary = deepSea100,

    background = dark80,
    onBackground = gray100,

    surface = dark60,
    surfaceTint = deepSea40,
    surfaceContainer = dark60,
    surfaceVariant = deepSea80,
    surfaceContainerHighest = dark80,
    surfaceContainerLow = dark80,
    onSurface = gray100,
    onSurfaceVariant = gray100,

    inverseSurface = gray10,
    inverseOnSurface = gray100,
    inversePrimary = seaBlue60,

    scrim = dark100,

    error = bloodyRed80,
    onError = gray100,
    errorContainer = bloodyRed40,
    onErrorContainer = gray100,

    outline = gray40,
    outlineVariant = gray10,
)

// ── 글자·아이콘 단계. 본문은 colorScheme.onSurface 다.
val contentSubtle = gray80
val contentMuted = gray60
val contentDisabled = gray40

val outlineSubtle = gray20

val emptyTrack = gray10

// ── 강조색 단계. 밝은 쪽부터.
val accentLight = seaBlue100
val accentMid = seaBlue60
val accentContent = seaBlue40
val accentDark = seaBlue20
val accentDeep = deepSea40
val accentContainer = deepSea80

/** 어두운 면 위의 파괴적 동작 글자. 채우는 버튼은 colorScheme.error 다. */
val destructiveContent = softRed

/** 시선을 붙잡아야 하는 표시. 글자에는 대비가 모자라 굵은 글자나 그래픽에만 쓴다. */
val attention = bloodyRed100
val onAttention = black

val selectedFill = gray100.copy(alpha = 0.08f)
val subtleFill = gray100.copy(alpha = 0.06f)

// ── 그림·scrim·파형처럼 테마 면이 아닌 어두운 바닥 위.
val overlayContent = white
val overlayContentSecondary = white.copy(alpha = 0.85f)
val overlayContentMuted = white.copy(alpha = 0.6f)
val overlayContentFaint = white.copy(alpha = 0.3f)

val overlayScrimSoft = dark100.copy(alpha = 0.3f)
val overlayScrim = dark100.copy(alpha = 0.5f)
val overlayScrimStrong = dark100.copy(alpha = 0.72f)

val artworkBackdropFallback = listOf(Color(0xFF4B453B), Color(0xFF1D1C1A), Color(0xFF0D0D0D))

/**
 * 앨범아트 대표색을 끌어올릴 때 대는 면. 썸네일 위 [overlayScrimStrong] 이 가장 밝아지는
 * 경우(흰 앨범아트)이고 [emptyTrack] 보다 불리해서, 이 하나로 재생바와 썸네일을 함께 만족시킨다.
 */
val accentContrastSurface = overlayScrimStrong.compositeOver(white)

/**
 * 대표색을 아직 읽지 못했을 때의 강조색. **회색이어야 한다** — 어느 대표색으로 바뀌어도
 * 색 변화로 읽히지 않는다. 테마 강조색을 두면 첫 재생에서 파랑이 곡 색으로 튀는 것이 보인다.
 */
val artworkAccentFallback = gray60
