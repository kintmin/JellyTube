package com.kintmin.domain.data_migration

import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationFailure
import com.kintmin.domain.data_migration.model.MigrationFailureReason
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.log.secret
import org.junit.Assert.assertEquals
import org.junit.Test

class MigrationLogTextTest {

    @Test
    fun `곡은 가릴 이름과 원본 주소와 파일 정보를 싣는다`() {
        val audio = MigrationAudioMedia(
            id = 1,
            source = "https://y/1",
            name = "곡",
            artist = "",
            description = "",
            rawAudioDurationSeconds = 200,
            audioFileName = "a.webm",
            imageFileName = null,
            lyricFileName = null,
            tjKaraokeNumber = null,
            editedAudioFileName = null,
            editedAudioDurationSeconds = null,
            createdTimeMillis = 0,
        )

        assertEquals("name=${secret("곡")} source=https://y/1 ext=webm duration=200s size=10", audio.logText(sizeBytes = 10))
    }

    @Test
    fun `요약은 실패한 곡 이름 대신 사유별 개수를 싣는다`() {
        val summary = MigrationSummary(
            newAudioMediaCount = 3,
            newPlaylistCount = 1,
            redownloadedCount = 0,
            failures = listOf(
                MigrationFailure("a", MigrationFailureReason.TRANSFER_FAILED),
                MigrationFailure("b", MigrationFailureReason.TRANSFER_FAILED),
            ),
        )

        assertEquals("새 곡=3 새 재생목록=1 다시 받음=0 실패={TRANSFER_FAILED=2}", summary.logText())
    }
}
