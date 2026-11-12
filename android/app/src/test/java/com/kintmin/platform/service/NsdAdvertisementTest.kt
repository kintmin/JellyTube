package com.kintmin.platform.service

import com.kintmin.fileshare.FileShareConstants
import com.kintmin.platform.service.FileShareForegroundService.Owner
import com.kintmin.presentation.port.model.DataMigrationSendStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class NsdAdvertisementTest {

    @Test
    fun `PC 수신만 켜져 있으면 PC 업로드 타입만 광고한다`() {
        assertEquals(
            setOf(FileShareConstants.NSD_SERVICE_TYPE),
            advertisedServiceTypes(setOf(Owner.PC_FILE_SHARE), DataMigrationSendStatus.Idle),
        )
    }

    @Test
    fun `페어링을 기다리는 동안은 PC 수신이 함께 켜져 있어도 이전 타입만 광고한다`() {
        assertEquals(
            setOf(FileShareConstants.MIGRATION_NSD_SERVICE_TYPE),
            advertisedServiceTypes(setOf(Owner.PC_FILE_SHARE, Owner.DATA_MIGRATION), DataMigrationSendStatus.WaitingForPair("123456")),
        )
    }

    @Test
    fun `페어링한 뒤에는 세션이 열려 있어도 아무것도 광고하지 않는다`() {
        val owners = setOf(Owner.PC_FILE_SHARE, Owner.DATA_MIGRATION)

        assertEquals(emptySet<String>(), advertisedServiceTypes(owners, DataMigrationSendStatus.Paired("iPhone")))
        assertEquals(emptySet<String>(), advertisedServiceTypes(owners, DataMigrationSendStatus.Finished(1, 0)))
    }

    @Test
    fun `이전 화면이 서버를 쓰더라도 세션이 닫혔으면 아무것도 광고하지 않는다`() {
        assertEquals(emptySet<String>(), advertisedServiceTypes(setOf(Owner.DATA_MIGRATION), DataMigrationSendStatus.Idle))
        assertEquals(emptySet<String>(), advertisedServiceTypes(setOf(Owner.DATA_MIGRATION), DataMigrationSendStatus.Locked))
    }

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
}
