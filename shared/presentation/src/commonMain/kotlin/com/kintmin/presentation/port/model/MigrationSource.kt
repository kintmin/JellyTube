package com.kintmin.presentation.port.model

/**
 * 이전 세션을 열어 둔 기기. [name] 은 보내는 기기가 광고한 이름이고 주소를 풀 때도 쓴다.
 * [isCompatible] 이 false 면 프로토콜 버전이 달라 고를 수 없다.
 */
data class MigrationSource(
    val name: String,
    val isCompatible: Boolean,
)
