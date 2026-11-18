package com.kintmin.domain.lyrics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsFileTest {

    @Test
    fun `확장자가 lrc 면 대소문자와 관계없이 싱크 가사다`() {
        assertTrue(LyricsFile.isSynced("/a/b.lrc"))
        assertTrue(LyricsFile.isSynced("b.LRC"))
        assertFalse(LyricsFile.isSynced("b.txt"))
        assertFalse(LyricsFile.isSynced("lrc"))
    }
}
