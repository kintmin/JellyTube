package com.kintmin.data.local_db

import com.kintmin.data.local_db.model.PlaylistEntity
import com.kintmin.data.local_db.model.clampText
import com.kintmin.domain.playlist.PlaylistTextLimit
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistEntityTextLimitTest {

    @Test
    fun `제목·설명을 도메인 상한으로 자르고 나머지는 건드리지 않는다`() {
        val entity = PlaylistEntity(
            name = "가".repeat(150),
            description = "a".repeat(5000),
            audioMediaCount = 3,
            rawPlayTimeDuration = 1000,
            isCustomImage = false,
        )

        val clamped = entity.clampText()

        assertEquals(PlaylistTextLimit.MAX_TITLE_LENGTH, clamped.name.length)
        assertEquals(PlaylistTextLimit.MAX_DESCRIPTION_LENGTH, clamped.description.length)
        assertEquals(entity.copy(name = clamped.name, description = clamped.description), clamped)
    }
}
