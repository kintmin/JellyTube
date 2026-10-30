package com.kintmin.data.local_db

import com.kintmin.data.local_db.mapper.toAudioEditTimelineOrNull
import com.kintmin.data.local_db.mapper.toTimelineJson
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AUDIO_EDIT_PROJECT.timelineJson 변환이 편집 상태를 그대로 보존하는지 검증한다. */
class AudioEditProjectMapperTest {

    private companion object {

        const val ORIGIN_DURATION_MS = 300_000L
    }

    @Test
    fun `저장했다가 읽으면 조각 배치와 병합 묶음, 끝 표시가 그대로 돌아온다`() {
        // 조각을 옮겨 맞붙이고 병합해 묶음까지 만든 상태
        val timeline = AudioEditTimeline.initial(ORIGIN_DURATION_MS)
            .split(100_000)
            .split(200_000)
            .delete(1)
            .move(index = 1, toPositionMs = 100_000)
            .merge(100_000)
            .withEnd(250_000)

        val restored = timeline.toTimelineJson().toAudioEditTimelineOrNull()

        assertEquals(timeline, restored)
    }

    @Test
    fun `모르는 필드가 있어도 읽는다`() {
        val json = """
            {"version":2,"originDurationMs":300000,"endMs":300000,"newField":"x",
             "clips":[{"sourceStartMs":0,"sourceEndMs":300000,"positionMs":0,"newClipField":1}]}
        """.trimIndent()

        assertEquals(AudioEditTimeline.initial(ORIGIN_DURATION_MS), json.toAudioEditTimelineOrNull())
    }

    @Test
    fun `읽을 수 없는 JSON이면 null이다`() {
        assertNull("not a json".toAudioEditTimelineOrNull())
    }
}
