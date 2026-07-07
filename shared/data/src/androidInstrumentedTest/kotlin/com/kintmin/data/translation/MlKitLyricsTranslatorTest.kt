package com.kintmin.data.translation

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * ML Kit 온디바이스 번역 계측 테스트.
 * 실기기/에뮬레이터에서 실행해야 한다 (Play Services + 최초 모델 다운로드 네트워크 필요).
 * 실행: ./gradlew.bat :shared:data:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MlKitLyricsTranslatorTest {

    private val translator = MlKitLyricsTranslator()

    @Test
    fun 영어_가사를_한국어로_번역한다() = runTest(timeout = 120.seconds) {
        val result = translator.translateToKorean("I wanna go to the moon", "en")

        assertTrue("번역은 성공해야 한다", result.isSuccess)
        val korean = result.getOrThrow()
        println("영어→한국어: $korean")
        assertTrue("결과에 한글이 포함되어야 한다", korean.any { it in '가'..'힣' })
    }

    @Test
    fun 일본어_가사를_한국어로_번역한다() = runTest(timeout = 120.seconds) {
        val result = translator.translateToKorean("君の名は", "ja")

        assertTrue("번역은 성공해야 한다", result.isSuccess)
        val korean = result.getOrThrow()
        println("일본어→한국어: $korean")
        assertTrue("결과에 한글이 포함되어야 한다", korean.any { it in '가'..'힣' })
    }

    @Test
    fun 지원하지_않는_언어코드는_Result_failure로_전파된다() = runTest(timeout = 30.seconds) {
        val result = translator.translateToKorean("hello", "xx")

        assertTrue("잘못된 언어 코드는 실패로 전파되어야 한다", result.isFailure)
    }
}
