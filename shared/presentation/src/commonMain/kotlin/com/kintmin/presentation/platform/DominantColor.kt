package com.kintmin.presentation.platform

/**
 * 이미지 파일의 대표 색을 ARGB 정수로 돌려준다. 읽을 수 없으면 null.
 *
 * 호출자는 null 일 때 기본값으로 떨어질 수 있어야 한다. 디코딩·색 추출 방식과
 * 어느 디스패처에서 도는지는 플랫폼마다 다르므로 전부 구현이 정한다
 * (`Dispatchers.IO` 는 JVM 전용이라 공통 코드에 둘 수 없다).
 */
expect suspend fun extractDominantColor(imageFileFullPath: String): Int?
