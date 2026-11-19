package com.kintmin.data.file_share.migration

import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.fileshare.MigrationFinishRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MigrationSessionTest {

    @Test
    fun `기본 코드는 숫자 여섯 자리이고 기본 토큰은 64자리 16진수다`() {
        val session = MigrationSession()

        assertEquals(6, session.code.length)
        assertTrue(session.code.all(Char::isDigit))
        val issued = assertIs<PairResult.Issued>(session.pair(session.code, "iPhone"))
        assertEquals(64, issued.token.length)
        assertTrue(session.isAuthorized(issued.token))
    }

    @Test
    fun `코드가 맞으면 그 토큰만 인가하고 틀린 토큰과 길이가 다른 토큰은 거절한다`() {
        val session = MigrationSession(code = "123456", generateToken = { "token" })

        assertEquals(PairResult.Issued("token"), session.pair("123456", "iPhone"))
        assertTrue(session.isAuthorized("token"))
        assertFalse(session.isAuthorized("tokes"))
        assertFalse(session.isAuthorized("token2"))
        assertFalse(session.isAuthorized(null))
    }

    @Test
    fun `코드를 다섯 번 틀리면 세션이 닫히고 닫힘을 한 번만 알린다`() {
        var closedCount = 0
        val session = MigrationSession(code = "123456", onClosed = { closedCount++ })

        repeat(MigrationSession.MAX_FAILED_ATTEMPTS) { session.pair("000000", "iPhone") }
        session.close()

        assertTrue(session.isClosed)
        assertEquals(1, closedCount)
        assertEquals(PairResult.Rejected, session.pair("123456", "iPhone"))
    }

    @Test
    fun `페어링한 뒤에는 맞는 코드가 Busy 이고 틀린 코드는 잠금으로 세지 않는다`() {
        val session = MigrationSession(code = "123456", generateToken = { "token" })
        session.pair("123456", "iPhone")

        assertEquals(PairResult.Busy, session.pair("123456", "Pixel"))
        repeat(MigrationSession.MAX_FAILED_ATTEMPTS) { assertEquals(PairResult.Rejected, session.pair("000000", "Pixel")) }
        assertFalse(session.isClosed)
    }

    @Test
    fun `manifest 로 내려준 파일만 받을 수 있다`() {
        val session = MigrationSession()
        session.allowFiles(setOf("AUDIO" to "a.m4a"))

        assertTrue(session.isFileAllowed("AUDIO", "a.m4a"))
        assertFalse(session.isFileAllowed("AUDIO", "b.m4a"))
    }

    @Test
    fun `상대 기기 이름은 제어 문자를 걷고 서로게이트 쌍을 쪼개지 않고 마흔 글자에서 자른다`() {
        assertEquals("iPhone", MigrationSession.toPeerName("iPh\u0000one\n"))
        assertEquals("😀".repeat(40), MigrationSession.toPeerName("😀".repeat(50)))
        assertTrue(MigrationSession.toPeerName(" \t\n").isNotBlank())
    }

    @Test
    fun `홀더는 세션을 열고 닫으며 상태를 함께 바꾼다`() {
        MigrationSessionHolder.close()
        val session = MigrationSessionHolder.open()

        assertEquals(DataMigrationSendStatus.WaitingForPair(session.code), MigrationSessionHolder.state.value)
        session.pair(session.code, "iPhone")
        assertEquals(DataMigrationSendStatus.Paired("iPhone"), MigrationSessionHolder.state.value)
        session.finish(MigrationFinishRequest(newAudioMediaCount = 3, failedCount = 1))
        assertEquals(DataMigrationSendStatus.Finished(3, 1), MigrationSessionHolder.state.value)

        MigrationSessionHolder.close()
        assertEquals(DataMigrationSendStatus.Idle, MigrationSessionHolder.state.value)
        assertNull(MigrationSessionHolder.session.value)
        assertTrue(session.isClosed)
    }
}
