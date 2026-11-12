package com.kintmin.platform.service.migration

import com.kintmin.fileshare.MigrationFinishRequest
import com.kintmin.presentation.port.model.DataMigrationSendStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationSessionHolderTest {

    @After
    fun tearDown() {
        MigrationSessionHolder.close()
    }

    @Test
    fun `코드 대기 → 연결 → 완료 순서로 상태가 바뀐다`() {
        val session = MigrationSessionHolder.open()
        assertEquals(DataMigrationSendStatus.WaitingForPair(session.code), MigrationSessionHolder.state.value)

        session.pair(session.code, "iPhone")
        assertEquals(DataMigrationSendStatus.Paired("iPhone"), MigrationSessionHolder.state.value)

        session.finish(MigrationFinishRequest(newAudioMediaCount = 3, failedCount = 1))
        assertEquals(DataMigrationSendStatus.Finished(3, 1), MigrationSessionHolder.state.value)
    }

    @Test
    fun `코드를 여러 번 틀리면 잠기고 세션을 비운다`() {
        val session = MigrationSessionHolder.open()

        repeat(MigrationSession.MAX_FAILED_ATTEMPTS) { session.pair("x", "iPhone") }

        assertEquals(DataMigrationSendStatus.Locked, MigrationSessionHolder.state.value)
        assertNull(MigrationSessionHolder.session.value)
    }

    @Test
    fun `닫으면 대기 상태로 돌아가고 이전 세션의 알림은 무시한다`() {
        val old = MigrationSessionHolder.open()
        old.pair(old.code, "iPhone")

        MigrationSessionHolder.close()
        old.finish(MigrationFinishRequest(1, 0))

        assertEquals(DataMigrationSendStatus.Idle, MigrationSessionHolder.state.value)
        assertTrue(old.isClosed)
    }
}
