package com.kintmin.domain.app_update

import com.kintmin.domain.app_update.model.AppRelease
import com.kintmin.domain.app_update.repository.AppUpdateRepository
import com.kintmin.domain.app_update.usecase.CheckAppUpdateOncePerDayUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckAppUpdateOncePerDayUseCaseTest {

    private val timeZone = TimeZone.UTC
    private val release = AppRelease(versionName = "2.1.0", downloadUrl = "https://example.com/app.apk")

    private fun repository(
        lastCheckMillis: Long? = null,
        latest: Result<AppRelease?> = Result.success(release),
        ignored: String? = null,
    ): AppUpdateRepository = mockk(relaxed = true) {
        coEvery { getLastCheckMillis() } returns lastCheckMillis
        coEvery { fetchLatestRelease() } returns latest
        coEvery { getIgnoredVersionName() } returns ignored
    }

    @Test
    fun `새 버전이 있으면 알리고 확인 시각을 남긴다`() = runTest {
        val repository = repository()

        val result = CheckAppUpdateOncePerDayUseCase(repository)("2.0.0", NOW, timeZone)

        assertEquals(release, result.getOrThrow())
        coVerify { repository.updateLastCheckMillis(NOW) }
    }

    @Test
    fun `같은 날 이미 확인했으면 네트워크를 타지 않는다`() = runTest {
        val repository = repository(lastCheckMillis = NOW - HOUR)

        val result = CheckAppUpdateOncePerDayUseCase(repository)("2.0.0", NOW, timeZone)

        assertNull(result.getOrThrow())
        coVerify(exactly = 0) { repository.fetchLatestRelease() }
    }

    @Test
    fun `날이 바뀌면 다시 확인한다`() = runTest {
        val repository = repository(lastCheckMillis = NOW - DAY)

        val result = CheckAppUpdateOncePerDayUseCase(repository)("2.0.0", NOW, timeZone)

        assertEquals(release, result.getOrThrow())
    }

    @Test
    fun `무시한 버전은 알리지 않는다`() = runTest {
        val repository = repository(ignored = "2.1.0")

        val result = CheckAppUpdateOncePerDayUseCase(repository)("2.0.0", NOW, timeZone)

        assertNull(result.getOrThrow())
    }

    @Test
    fun `무시한 뒤 더 새 버전이 나오면 다시 알린다`() = runTest {
        val repository = repository(ignored = "2.0.5")

        val result = CheckAppUpdateOncePerDayUseCase(repository)("2.0.0", NOW, timeZone)

        assertEquals(release, result.getOrThrow())
    }

    @Test
    fun `최신이면 알리지 않는다`() = runTest {
        val repository = repository()

        val result = CheckAppUpdateOncePerDayUseCase(repository)("2.1.0", NOW, timeZone)

        assertNull(result.getOrThrow())
    }

    @Test
    fun `확인에 실패하면 시각을 남기지 않아 다음에 다시 확인한다`() = runTest {
        val repository = repository(latest = Result.failure(RuntimeException("offline")))

        val result = CheckAppUpdateOncePerDayUseCase(repository)("2.0.0", NOW, timeZone)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.updateLastCheckMillis(any()) }
    }

    private companion object {
        const val HOUR = 60 * 60 * 1000L
        const val DAY = 24 * HOUR
        // 2026-11-22T12:00:00Z
        const val NOW = 1_795_348_800_000L
    }
}
