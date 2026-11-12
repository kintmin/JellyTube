package com.kintmin.ios.migration

import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.presentation.port.model.MigrationSource
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosMigrationSourceDiscoveryTest {

    private val bridge = FakeBrowserBridge()
    private val discovery = IosMigrationSourceDiscovery(bridge)

    @Test
    fun `이전 타입만 찾고 TXT 의 버전으로 고를 수 있는지 정한다`() = runBlocking {
        bridge.browsed = listOf(
            service("Galaxy S24", FileShareConstants.MIGRATION_FORMAT_VERSION.toString()),
            service("Pixel 9", (FileShareConstants.MIGRATION_FORMAT_VERSION + 1).toString()),
        )

        val sources = discovery.discover()

        assertEquals(FileShareConstants.MIGRATION_NSD_SERVICE_TYPE, bridge.browsedType)
        assertEquals(
            listOf(MigrationSource("Galaxy S24", isCompatible = true), MigrationSource("Pixel 9", isCompatible = false)),
            sources,
        )
    }

    @Test
    fun `TXT 에 버전이 없으면 고를 수 있게 둔다`() = runBlocking {
        bridge.browsed = listOf(IosBrowsedService("Galaxy S24", emptyMap()))

        assertEquals(listOf(MigrationSource("Galaxy S24", isCompatible = true)), discovery.discover())
    }

    @Test
    fun `고른 기기의 이름으로 주소를 푼다`() = runBlocking {
        bridge.resolved = mapOf("Galaxy S24" to IosResolvedService("192.168.0.2", 52847))

        assertEquals(
            MigrationEndpoint("192.168.0.2", 52847),
            discovery.resolve(MigrationSource("Galaxy S24", isCompatible = true)),
        )
        assertNull(discovery.resolve(MigrationSource("Pixel 9", isCompatible = true)))
    }

    private fun service(name: String, version: String) =
        IosBrowsedService(name, mapOf(FileShareConstants.MIGRATION_TXT_FORMAT_VERSION to version))
}

private class FakeBrowserBridge : IosMigrationSourceBrowserBridge {
    var browsed: List<IosBrowsedService> = emptyList()
    var resolved: Map<String, IosResolvedService> = emptyMap()
    var browsedType: String? = null

    override suspend fun browse(serviceType: String, settleMillis: Long, timeoutMillis: Long): List<IosBrowsedService> {
        browsedType = serviceType
        return browsed
    }

    override suspend fun resolve(serviceType: String, name: String, timeoutMillis: Long): IosResolvedService? =
        resolved[name]
}
