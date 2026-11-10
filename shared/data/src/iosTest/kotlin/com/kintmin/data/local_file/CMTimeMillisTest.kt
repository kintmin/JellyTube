package com.kintmin.data.local_file

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreMedia.CMTimeMake
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalForeignApi::class)
class CMTimeMillisTest {

    @Test
    fun `타임스케일로 나눈 초를 밀리초로 돌려준다`() {
        assertEquals(19_000L, CMTimeMake(19_000, 1_000).toMillisOrNull())
        assertEquals(19_000L, CMTimeMake(19, 1).toMillisOrNull())
        assertEquals(4_643L, CMTimeMake(204_800, 44_100).toMillisOrNull())
    }

    @Test
    fun `타임스케일을 무시하면 틀리는 값을 잡는다`() {
        // 44.1kHz 샘플 단위로 들어온 길이를 그대로 읽으면 수천 배가 된다.
        assertEquals(252_000L, CMTimeMake(11_113_200, 44_100).toMillisOrNull())
    }

    @Test
    fun `길이를 못 읽으면 null 이다`() {
        assertNull(CMTimeMake(0, 1_000).toMillisOrNull())
        assertNull(CMTimeMake(1_000, 0).toMillisOrNull())
        assertNull(CMTimeMake(-1_000, 1_000).toMillisOrNull())
    }
}
