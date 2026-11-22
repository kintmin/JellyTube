package com.kintmin.data.translation

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * ML Kit 온디바이스 감지·번역 계측 테스트.
 * 실기기/에뮬레이터에서 실행해야 한다 (Play Services + 최초 모델 다운로드 네트워크 필요).
 * 실행: ./gradlew.bat :shared:data:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MlKitLyricsTranslationEngineTest {

    private val engine = MlKitLyricsTranslationEngine()

    @Test
    fun 줄마다_언어를_감지한다() = runTest(timeout = 60.seconds) {
        val languages = engine.detectLanguages(
            listOf("I wanna go to the moon tonight", "君の名前を教えてください", "보고 싶은 마음이 너무 커요"),
        )

        assertEquals(listOf("en", "ja", "ko"), languages)
    }

    @Test
    fun 영어_가사를_한국어로_번역한다() = runTest(timeout = 120.seconds) {
        val korean = engine.translateToKorean(listOf("I wanna go to the moon"), "en").single()

        println("영어→한국어: $korean")
        assertTrue("결과에 한글이 포함되어야 한다", korean.any { it in '가'..'힣' })
    }

    @Test
    fun 일본어_가사를_한국어로_번역한다() = runTest(timeout = 120.seconds) {
        val korean = engine.translateToKorean(listOf("君の名は"), "ja").single()

        println("일본어→한국어: $korean")
        assertTrue("결과에 한글이 포함되어야 한다", korean.any { it in '가'..'힣' })
    }

    @Test
    fun 번역하지_못하는_언어는_원문을_돌려준다() = runTest(timeout = 30.seconds) {
        val result = engine.translateToKorean(listOf("hello"), "xx")

        assertEquals(listOf("hello"), result)
    }
}
