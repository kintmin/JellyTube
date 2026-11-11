package com.kintmin.platform.service.migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationSessionTest {

    private fun session() = MigrationSession(code = "123456", generateToken = { "token" })

    @Test
    fun `코드가 맞으면 토큰을 주고 그 토큰만 인가한다`() {
        val session = session()

        assertEquals("token", session.pair("123456"))
        assertTrue(session.isAuthorized("token"))
        assertFalse(session.isAuthorized("other"))
        assertFalse(session.isAuthorized(null))
    }

    @Test
    fun `페어링 전에는 아무 토큰도 인가하지 않는다`() {
        assertFalse(session().isAuthorized("token"))
    }

    @Test
    fun `이미 페어링한 세션은 같은 코드로도 다시 토큰을 주지 않는다`() {
        val session = session()
        session.pair("123456")

        assertNull(session.pair("123456"))
    }

    @Test
    fun `코드를 정해진 횟수만큼 틀리면 세션이 닫혀 맞는 코드도 거부한다`() {
        val session = session()
        repeat(MigrationSession.MAX_FAILED_ATTEMPTS - 1) { assertNull(session.pair("000000")) }
        assertFalse(session.isClosed)

        assertNull(session.pair("000000"))

        assertTrue(session.isClosed)
        assertNull(session.pair("123456"))
    }

    @Test
    fun `닫힌 세션은 발급한 토큰도 인가하지 않는다`() {
        val session = session()
        session.pair("123456")

        session.close()

        assertFalse(session.isAuthorized("token"))
    }

    @Test
    fun `허용한 파일만 받아 갈 수 있다`() {
        val session = session()
        assertFalse(session.isFileAllowed("AUDIO", "a.m4a"))

        session.allowFiles(setOf("AUDIO" to "a.m4a"))

        assertTrue(session.isFileAllowed("AUDIO", "a.m4a"))
        assertFalse(session.isFileAllowed("IMAGE", "a.m4a"))
        assertFalse(session.isFileAllowed("AUDIO", "../a.m4a"))
    }

    @Test
    fun `닫힐 때 한 번만 알린다`() {
        var closedCount = 0
        val session = MigrationSession(code = "123456", generateToken = { "token" }, onClosed = { closedCount++ })

        repeat(MigrationSession.MAX_FAILED_ATTEMPTS + 2) { session.pair("000000") }
        session.close()

        assertEquals(1, closedCount)
    }

    @Test
    fun `기본 코드는 여섯 자리 숫자다`() {
        repeat(50) { assertTrue(MigrationSession().code.matches(Regex("\\d{6}"))) }
    }
}
