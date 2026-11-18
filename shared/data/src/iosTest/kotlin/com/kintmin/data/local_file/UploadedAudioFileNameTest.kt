package com.kintmin.data.local_file

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UploadedAudioFileNameTest {

    @Test
    fun `마지막 점 뒤가 확장자고 그 앞이 대체 제목이다`() {
        val name = UploadedAudioFileName("라이브 1.2 버전.m4a")

        assertEquals("m4a", name.ext)
        assertEquals("라이브 1.2 버전", name.fallbackTitle)
    }

    @Test
    fun `확장자가 없는 이름은 받지 않는다`() {
        assertFailsWith<IllegalArgumentException> { UploadedAudioFileName("noext") }
        assertFailsWith<IllegalArgumentException> { UploadedAudioFileName("trailing.") }
    }
}
