package com.kintmin.domain.step.usecase

import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ResetDataOncePerDayUseCaseTest {

    private val registerWorker: RegisterDailyResetWorkerUseCase = mockk(relaxed = true)
    private val zoneId = TimeZone.UTC
    private val today = Clock.System.now().toLocalDateTime(zoneId).date.toEpochDays().toLong()

    private fun buildUseCase(initialEpochDay: Long = today): ResetDataOncePerDayUseCase =
        ResetDataOncePerDayUseCase(registerWorker).also { it.cachedEpochDay.value = initialEpochDay }

    // ─── 기본 동작 ──────────────────────────────────────────────────────

    @Test
    fun `같은 날 호출 시 resetAction이 실행되지 않는다`() {
        val useCase = buildUseCase(initialEpochDay = today)
        var resetCount = 0

        useCase(0, null, zoneId) { resetCount++ }

        assertEquals(0, resetCount)
    }

    @Test
    fun `다음 날 단일 호출 시 resetAction이 1회 실행된다`() {
        val useCase = buildUseCase(initialEpochDay = today - 1)
        var resetCount = 0

        useCase(0, null, zoneId) { resetCount++ }

        assertEquals(1, resetCount)
    }

    @Test
    fun `다음 날 호출 시 registerDailyResetWorkerUseCase가 1회 호출된다`() {
        val useCase = buildUseCase(initialEpochDay = today - 1)

        useCase(0, null, zoneId) {}

        verify(exactly = 1) { registerWorker(any(), any(), any()) }
    }

    @Test
    fun `연속 두 번 호출 시 resetAction은 첫 번째 호출에만 1회 실행된다`() {
        val useCase = buildUseCase(initialEpochDay = today - 1)
        var resetCount = 0

        useCase(0, null, zoneId) { resetCount++ }
        useCase(0, null, zoneId) { resetCount++ }

        assertEquals(1, resetCount)
    }

    // ─── 동시성 ─────────────────────────────────────────────────────────

    @Test
    fun `동시 다발 호출 시 resetAction은 정확히 1번만 실행된다`() {
        val useCase = buildUseCase(initialEpochDay = today - 1)
        val resetCount = AtomicInteger(0)

        val threads = List(100) {
            Thread { useCase(0, null, zoneId) { resetCount.incrementAndGet() } }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(1, resetCount.get())
    }

    @Test
    fun `동시 다발 호출 시 registerDailyResetWorkerUseCase는 정확히 1번만 호출된다`() {
        val useCase = buildUseCase(initialEpochDay = today - 1)

        val threads = List(100) {
            Thread { useCase(0, null, zoneId) {} }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        verify(exactly = 1) { registerWorker(any(), any(), any()) }
    }
}
