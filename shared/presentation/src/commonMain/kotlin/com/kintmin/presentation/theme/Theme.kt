package com.kintmin.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = seaBlue10,    // 배경과 구분되는 강조색, Button, navigation
    onPrimary = white,
    primaryContainer = deepSea80,   // floating
    onPrimaryContainer = gray100,

    secondary = deepSea80,
    onSecondary = gray100,
    secondaryContainer = deepSea80, // 네비게이션바 클릭색상
    onSecondaryContainer = gray100,

    // 켜짐·재생 중 강조. primary 는 어두운 배경과 scrim 위에서 꺼짐보다 어둡게 보인다.
    tertiary = seaBlue80,
    onTertiary = deepSea100,

    background = dark80,
    onBackground = gray100,

    surface = dark60,
    surfaceTint = deepSea40,
    surfaceContainer = dark60,   // 네비게이션바 배경색상
    surfaceVariant = deepSea80, // textField
    surfaceContainerHighest = dark80,
    surfaceContainerLow = dark80,   // elevatedButton
    onSurface = gray100,
    onSurfaceVariant = gray100,

    // 비워 두면 M3 기본 보라 팔레트로 떨어진다. Snackbar 가 이 셋을 쓴다.
    inverseSurface = gray10,
    inverseOnSurface = gray100,
    inversePrimary = seaBlue60,

    scrim = dark100,

    error = bloodyRed80,
    onError = gray100,
    onErrorContainer = gray100,

    outline = gray40,
    outlineVariant = gray10,
)

private val LightColorScheme = lightColorScheme()

@Composable
fun JellyTubeTheme(
    darkTheme: Boolean = true, //isSystemInDarkTheme()
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}