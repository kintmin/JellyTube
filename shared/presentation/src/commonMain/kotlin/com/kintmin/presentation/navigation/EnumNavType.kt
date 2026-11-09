package com.kintmin.presentation.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write

/**
 * `enum` 라우트 인자용 [NavType].
 *
 * **이것이 없으면 iOS 에서 그래프를 만드는 순간 죽는다.** Navigation 은 기본 타입이 아닌
 * 인자의 [NavType] 을 리플렉션으로 추론하는데 Kotlin/Native 에는 리플렉션이 없어
 * `could not find any NavType for argument ...` 로 터진다. 안드로이드의 `NavType.EnumType`
 * 은 `java.lang.Class` 를 쓰는 JVM 전용이라 공통 코드에서 쓸 수 없다.
 *
 * 그래서 `enum` 을 인자로 받는 라우트는 **등록부와 `toRoute()` 양쪽에 `typeMap` 을 넘겨야
 * 한다.** 한쪽만 넘기면 안드로이드에서는 지나가고 iOS 에서만 터진다.
 *
 * 직렬화 형태는 `name` 이다. 순서(ordinal)를 쓰면 enum 에 항목을 끼워 넣는 순간 기존 링크가
 * 다른 화면을 연다.
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
