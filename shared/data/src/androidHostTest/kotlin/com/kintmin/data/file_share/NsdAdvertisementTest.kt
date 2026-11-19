package com.kintmin.data.file_share

import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.fileshare.FileShareConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class NsdAdvertisementTest {

    @Test
    fun `PC 수신만 켜져 있으면 PC 업로드 타입만 광고한다`() {
        assertEquals(
            setOf(FileShareConstants.NSD_SERVICE_TYPE),
            advertisedServiceTypes(setOf(FileShareServerOwner.PC_FILE_SHARE), DataMigrationSendStatus.Idle),
        )
    }

    @Test
    fun `페어링을 기다리는 동안은 PC 수신이 함께 켜져 있어도 이전 타입만 광고한다`() {
        assertEquals(
            setOf(FileShareConstants.MIGRATION_NSD_SERVICE_TYPE),
            advertisedServiceTypes(setOf(FileShareServerOwner.PC_FILE_SHARE, FileShareServerOwner.DATA_MIGRATION), DataMigrationSendStatus.WaitingForPair("123456")),
        )
    }

    @Test
    fun `페어링한 뒤에는 세션이 열려 있어도 아무것도 광고하지 않는다`() {
        val owners = setOf(FileShareServerOwner.PC_FILE_SHARE, FileShareServerOwner.DATA_MIGRATION)

        assertEquals(emptySet<String>(), advertisedServiceTypes(owners, DataMigrationSendStatus.Paired("iPhone")))
        assertEquals(emptySet<String>(), advertisedServiceTypes(owners, DataMigrationSendStatus.Finished(1, 0)))
    }

    @Test
    fun `이전 화면이 서버를 쓰더라도 세션이 닫혔으면 아무것도 광고하지 않는다`() {
        assertEquals(emptySet<String>(), advertisedServiceTypes(setOf(FileShareServerOwner.DATA_MIGRATION), DataMigrationSendStatus.Idle))
        assertEquals(emptySet<String>(), advertisedServiceTypes(setOf(FileShareServerOwner.DATA_MIGRATION), DataMigrationSendStatus.Locked))
    }

    @Test
    fun `이전 타입은 보내는 기기 인스턴스 이름과 버전 TXT 를 광고한다`() {
        val advertised = advertisedServiceOf(FileShareConstants.MIGRATION_NSD_SERVICE_TYPE, "iPhone (a1b2c3)")

        assertEquals("iPhone (a1b2c3)", advertised.name)
        assertEquals(
            mapOf(FileShareConstants.MIGRATION_TXT_FORMAT_VERSION to FileShareConstants.MIGRATION_FORMAT_VERSION.toString()),
            advertised.txtRecord,
        )
    }

    @Test
    fun `PC 업로드 타입은 고정 이름이고 TXT 가 없다`() {
        val advertised = advertisedServiceOf(FileShareConstants.NSD_SERVICE_TYPE, "iPhone (a1b2c3)")

        assertEquals(FileShareConstants.NSD_SERVICE_NAME, advertised.name)
        assertEquals(emptyMap<String, String>(), advertised.txtRecord)
    }
}
