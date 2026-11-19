package com.kintmin.presentation.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShareFileNameTest {

    @Test
    fun `아티스트와 제목을 하이픈으로 잇고 확장자를 붙인다`() {
        assertEquals("아이유 - 좋은 날.m4a", shareFileName(title = "좋은 날", artist = "아이유", ext = "m4a"))
    }

    @Test
    fun `아티스트가 비어 있으면 제목만 쓴다`() {
        assertEquals("좋은 날.m4a", shareFileName(title = "좋은 날", artist = " ", ext = "m4a"))
    }

    @Test
    fun `파일 이름에 못 쓰는 문자는 밑줄로 바꾼다`() {
        assertEquals("A_B_C.m4a", shareFileName(title = "A/B:C", artist = "", ext = "m4a"))
    }

    @Test
    fun `제목과 아티스트가 모두 비면 기본 이름을 쓴다`() {
        assertEquals("JellyTube.m4a", shareFileName(title = "", artist = "", ext = "m4a"))
    }

    @Test
    fun `끝 점은 잘라낸다`() {
        assertEquals("끝.m4a", shareFileName(title = "끝...", artist = "", ext = "m4a"))
    }

    @Test
    fun `긴 이름은 80자로 자른다`() {
        val name = shareFileName(title = "가".repeat(200), artist = "", ext = "m4a")

        assertEquals("가".repeat(80) + ".m4a", name)
    }

    @Test
    fun `서러게이트 쌍 중간에서 자르지 않는다`() {
        val name = shareFileName(title = "가".repeat(79) + "😀", artist = "", ext = "m4a")

        assertTrue(name.removeSuffix(".m4a").none { it.isHighSurrogate() })
    }
}
