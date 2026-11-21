package com.kintmin.data.python_bridge

import com.kintmin.domain.audio_media.model.AgeRestrictedMedia
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class YtDlpErrorTest {

    @Test
    fun `연령 제한 오류만 도메인 예외로 바꾸고 나머지는 그대로 둔다`() {
        val ageRestricted = Exception(
            "Download failed: ERROR: [youtube] OT2IKn6ycbA: Sign in to confirm your age. This video may be inappropriate for some users.",
        )
        val other = Exception("Download failed: ERROR: [youtube] abc: HTTP Error 429")

        assertTrue(YtDlpError.classify(ageRestricted) is AgeRestrictedMedia)
        assertSame(other, YtDlpError.classify(other))
    }
}
