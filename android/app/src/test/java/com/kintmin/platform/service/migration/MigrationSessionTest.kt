package com.kintmin.platform.service.migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationSessionTest {

    private fun session() = MigrationSession(code = "123456", generateToken = { "token" })

    @Test
    fun `코드가 맞으면 토큰을 주고 그 토큰만 인가한다`() {
        val session = session()

        assertEquals(PairResult.Issued("token"), session.pair("123456", DEVICE))
        assertTrue(session.isAuthorized("token"))
        assertFalse(session.isAuthorized("other"))
        assertFalse(session.isAuthorized(null))
    }

    @Test
    fun `페어링하면 상대 기기 이름을 알린다`() {
        var pairedName: String? = null
        val session = MigrationSession(code = "123456", generateToken = { "token" }, onPaired = { pairedName = it })

        session.pair("123456", DEVICE)

        assertEquals(DEVICE, pairedName)
    }

    @Test
    fun `페어링 전에는 아무 토큰도 인가하지 않는다`() {
        assertFalse(session().isAuthorized("token"))
    }

    @Test
    fun `이미 페어링한 세션은 같은 코드로도 다시 토큰을 주지 않는다`() {
        val session = session()
        session.pair("123456", DEVICE)

        assertEquals(PairResult.Busy, session.pair("123456", DEVICE))
    }

    @Test
    fun `페어링한 뒤 틀린 코드는 코드 틀림으로 답하되 잠금으로 세지 않아 받는 기기가 끊기지 않는다`() {
        val session = session()
        session.pair("123456", DEVICE)

        repeat(MigrationSession.MAX_FAILED_ATTEMPTS) { assertEquals(PairResult.Rejected, session.pair("000000", DEVICE)) }

        assertFalse(session.isClosed)
        assertTrue(session.isAuthorized("token"))
    }

    @Test
    fun `코드를 정해진 횟수만큼 틀리면 세션이 닫혀 맞는 코드도 거부한다`() {
        val session = session()
        repeat(MigrationSession.MAX_FAILED_ATTEMPTS - 1) { assertEquals(PairResult.Rejected, session.pair("000000", DEVICE)) }
        assertFalse(session.isClosed)

        assertEquals(PairResult.Rejected, session.pair("000000", DEVICE))

        assertTrue(session.isClosed)
        assertEquals(PairResult.Rejected, session.pair("123456", DEVICE))
    }

    @Test
    fun `닫힌 세션은 발급한 토큰도 인가하지 않는다`() {
        val session = session()
        session.pair("123456", DEVICE)

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

        repeat(MigrationSession.MAX_FAILED_ATTEMPTS + 2) { session.pair("000000", DEVICE) }
        session.close()

        assertEquals(1, closedCount)
    }

    @Test
    fun `상대 기기 이름은 제어 문자를 걷고 줄여서 알린다`() {
        var pairedName: String? = null
        val session = MigrationSession(code = "123456", generateToken = { "token" }, onPaired = { pairedName = it })

        session.pair("123456", "  Kim's\niPhone\u0000 ")

        assertEquals("Kim'siPhone", pairedName)
    }

    @Test
    fun `상대 기기 이름이 길면 서로게이트 쌍을 쪼개지 않고 자른다`() {
        val name = MigrationSession.toPeerName("😀".repeat(50))

        assertEquals("😀".repeat(40), name)
    }

    @Test
    fun `상대 기기 이름이 비면 대신 쓸 이름을 준다`() {
        assertTrue(MigrationSession.toPeerName(" \t\n").isNotBlank())
    }

    @Test
    fun `기본 코드는 여섯 자리 숫자다`() {
        repeat(50) { assertTrue(MigrationSession().code.matches(Regex("\\d{6}"))) }
    }
}

private const val DEVICE = "iPhone"
