package com.kintmin.jellytube

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.jellytube.python_bridge_impl.LyricsTransliteratorImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * Chaquopy(Python) 기반 음차 계측 테스트.
 * 실기기/에뮬레이터에서 실행해야 한다 (Python 런타임 필요).
 * 실행: ./gradlew.bat :android:app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class LyricsTransliteratorTest {

    private val transliterator =
        LyricsTransliteratorImpl(ApplicationProvider.getApplicationContext())

    @Test
    fun 일본어_가사를_한글_발음으로_음차한다() = runTest(timeout = 60.seconds) {
        val result = transliterator.transliterateToKorean("君の名は")

        assertTrue("음차는 성공해야 한다", result.isSuccess)
        val hangul = result.getOrThrow()
        println("음차 결과: $hangul")
        // 결과에 한글이 하나라도 포함되어야 한다.
        assertTrue("결과에 한글이 포함되어야 한다", hangul.any { it in '가'..'힣' })
    }

    @Test
    fun 여러_줄_가사의_줄_구조가_보존된다() = runTest(timeout = 60.seconds) {
        val result = transliterator.transliterateToKorean("君の名は\nさようなら")

        assertTrue(result.isSuccess)
        val lines = result.getOrThrow().split("\n")
        assertTrue("입력 2줄이면 출력도 2줄이어야 한다", lines.size == 2)
    }

    @Test
    fun 빈_문자열이면_예외가_Result_failure로_전파된다() = runTest(timeout = 60.seconds) {
        val result = transliterator.transliterateToKorean("   ")

        // Python 의 ValueError 가 PyException -> Result.failure 로 전파되어야 한다.
        assertTrue("빈 입력은 실패로 전파되어야 한다", result.isFailure)
    }
}
