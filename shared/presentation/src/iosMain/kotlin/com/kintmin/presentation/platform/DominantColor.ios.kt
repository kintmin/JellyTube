package com.kintmin.presentation.platform

/**
 * iOS 대표 색 추출은 `CGImage` 를 직접 샘플링하거나 CoreImage 로 평균색을 구해야 한다.
 *
 * 호출자가 null 을 기본 그라데이션으로 처리하므로 그때까지 null 을 돌려준다.
 * 아무 색이나 돌려주면 앨범아트와 어긋난 배경이 나와 더 나쁘다.
 */
actual suspend fun extractDominantColor(imageFileFullPath: String): Int? = null
