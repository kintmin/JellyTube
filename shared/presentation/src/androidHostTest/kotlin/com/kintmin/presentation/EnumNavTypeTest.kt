package com.kintmin.presentation

import com.kintmin.presentation.navigation.EnumNavType
import com.kintmin.presentation.ui.main.MainTabItem
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import com.kintmin.presentation.ui.setting.data_migration_send.navigation.SettingDataMigrationSendScreenRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

class EnumNavTypeTest {

    private val navType = EnumNavType(MainTabItem.entries)

    @Test
    fun `순서가 아니라 이름으로 직렬화한다`() {
        // ordinal 로 바꾸면 enum 에 항목을 끼워 넣는 순간 기존 딥링크가 다른 화면을 연다.
        MainTabItem.entries.forEach { tab ->
            assertEquals(tab.name, navType.serializeAsValue(tab))
        }
    }

    @Test
    fun `직렬화한 값을 그대로 되읽는다`() {
        MainTabItem.entries.forEach { tab ->
            assertEquals(tab, navType.parseValue(navType.serializeAsValue(tab)))
        }
    }

    @Test
    fun `모르는 값은 예외로 올린다`() {
        assertThrows(IllegalArgumentException::class.java) { navType.parseValue("Unknown") }
    }

    @Test
    fun `enum 인자를 받는 라우트는 EnumNavType 이 등록된 것뿐이다`() {
        // 등록이 빠진 enum 인자는 리플렉션이 없는 iOS 에서 그래프 생성을 터뜨린다.
        // Android 는 리플렉션으로 넘어가 터지지 않으므로 여기서 잡는다.
        val enumArguments = MainScreenRoute::class.constructors.first().parameters
            .mapNotNull { it.type.classifier as? KClass<*> }
            .filter { it.java.isEnum }

        assertEquals(listOf(MainTabItem::class), enumArguments)
    }

    @Test
    fun `라우트의 enum 인자는 Serializable 이다`() {
        // 없으면 JVM 은 리플렉션으로 넘어가지만 Kotlin/Native 는 KSerializer 를 못 찾아 그래프 생성이 터진다.
        val enumArguments = listOf(MainScreenRoute::class, SettingDataMigrationSendScreenRoute::class)
            .flatMap { route -> route.constructors.first().parameters.mapNotNull { it.type.classifier as? KClass<*> } }
            .filter { it.java.isEnum }

        assertTrue(enumArguments.isNotEmpty())
        enumArguments.forEach { assertTrue("$it", it.java.isAnnotationPresent(Serializable::class.java)) }
    }
}
