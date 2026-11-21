package com.kintmin.presentation.platform

/** 대표 색을 못 뽑았을 때의 값. 앱 기본 그라데이션 최상단 색이다. */
const val FALLBACK_DOMINANT_COLOR: Int = 0xFF4B453B.toInt()

/** ARGB 정수. 읽을 수 없으면 null. 디스패처는 구현이 정한다(`Dispatchers.IO` 는 JVM 전용). */
expect suspend fun extractDominantColor(imageFileFullPath: String): Int?
