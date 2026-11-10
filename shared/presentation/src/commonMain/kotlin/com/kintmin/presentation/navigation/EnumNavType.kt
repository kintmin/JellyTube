package com.kintmin.presentation.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write

/**
 * `enum` 라우트 인자용 [NavType]. 동작 규약은 `EnumNavTypeTest` 가 고정한다.
 *
 * Navigation 은 기본 타입이 아닌 인자의 [NavType] 을 리플렉션으로 추론하는데 Kotlin/Native
 * 에는 리플렉션이 없고, `NavType.EnumType` 은 `java.lang.Class` 를 쓰는 JVM 전용이다.
 * `enum` 인자를 받는 라우트는 **등록부와 `toRoute()` 양쪽에** `typeMap` 을 넘겨야 한다.
 */
class EnumNavType<T : Enum<T>>(
    private val entries: List<T>,
) : NavType<T>(isNullableAllowed = false) {

    override fun put(bundle: SavedState, key: String, value: T) {
        bundle.write { putString(key, value.name) }
    }

    override fun get(bundle: SavedState, key: String): T? =
        bundle.read { getStringOrNull(key) }?.let(::parseValue)

    override fun parseValue(value: String): T =
        entries.firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("$value 는 ${entries.firstOrNull()?.let { it::class.simpleName }} 의 값이 아닙니다.")

    override fun serializeAsValue(value: T): String = value.name
}
