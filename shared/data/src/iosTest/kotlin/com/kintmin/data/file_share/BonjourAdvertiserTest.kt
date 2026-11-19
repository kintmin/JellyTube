package com.kintmin.data.file_share

import com.kintmin.fileshare.FileShareConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BonjourAdvertiserTest {

    private val migration = setOf(FileShareConstants.MIGRATION_NSD_SERVICE_TYPE)
    private val active = mutableSetOf<String>()

    private fun advertiser(testScope: TestScope) = BonjourAdvertiser(
        scope = CoroutineScope(StandardTestDispatcher(testScope.testScheduler)),
        resolveMigrationInstanceName = { "iPhone (a1b2c3)" },
        register = { serviceType, _ ->
            AdvertisementRegistration(
                publish = { active += serviceType },
                stop = { active -= serviceType },
            )
        },
    )

    @Test
    fun `광고를 켜면 서비스 타입이 등록된다`() = runTest {
        val advertiser = advertiser(this)

        advertiser.start(flowOf(migration))
        advanceUntilIdle()

        assertEquals(migration, active)
    }

    @Test
    fun `멈추면 등록을 모두 내린다`() = runTest {
        val advertiser = advertiser(this)
        advertiser.start(flowOf(migration))
        advanceUntilIdle()

        advertiser.stop()
        advanceUntilIdle()

        assertEquals(emptySet(), active)
    }

    @Test
    fun `멈춘 직후 다시 켜도 광고가 등록된다`() = runTest {
        val advertiser = advertiser(this)
        advertiser.start(flowOf(migration))
        advanceUntilIdle()

        advertiser.stop()
        advertiser.start(flowOf(migration))
        advanceUntilIdle()

        assertEquals(migration, active)
    }
}
