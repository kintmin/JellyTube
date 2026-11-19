package com.kintmin.data.data_migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MigrationInstanceNameTest {

    @Test
    fun `인스턴스 이름은 63바이트를 넘지 않고 한글을 중간에서 자르지 않는다`() {
        val name = "가".repeat(30)

        val instanceName = toDnsSdInstanceName(name)

        assertEquals("가".repeat(21), instanceName)
        assertEquals(63, instanceName.encodeToByteArray().size)
    }

    @Test
    fun `서로게이트 쌍은 쪼개지 않는다`() {
        val name = "a".repeat(60) + "😀"

        assertEquals("a".repeat(60), toDnsSdInstanceName(name))
    }

    @Test
    fun `짧은 이름은 그대로 둔다`() {
        assertEquals("Galaxy S24", toDnsSdInstanceName("Galaxy S24"))
    }

    @Test
    fun `기기 이름 뒤에 식별자 앞 여섯 자를 붙인다`() {
        assertEquals(
            "iPhone (a1b2c3)",
            toMigrationInstanceName("iPhone", "a1b2c3d4-0000-4000-8000-000000000000"),
        )
    }

    @Test
    fun `이름이 같은 두 기기도 식별자가 다르면 이름이 다르다`() {
        assertNotEquals(
            toMigrationInstanceName("iPhone", "a1b2c3d4-0000-4000-8000-000000000000"),
            toMigrationInstanceName("iPhone", "ffffffff-0000-4000-8000-000000000000"),
        )
    }

    @Test
    fun `이름이 길면 식별자가 남도록 기기 이름을 자른다`() {
        val instanceName = toMigrationInstanceName("가".repeat(30), "a1b2c3d4-0000-4000-8000-000000000000")

        assertEquals("가".repeat(18) + " (a1b2c3)", instanceName)
        assertEquals(true, instanceName.encodeToByteArray().size <= 63)
    }

    @Test
    fun `식별자가 없으면 기기 이름만 쓴다`() {
        assertEquals("Galaxy S24", toMigrationInstanceName("Galaxy S24", null))
        assertEquals("Galaxy S24", toMigrationInstanceName("Galaxy S24", ""))
    }
}
