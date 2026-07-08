package com.kintmin.data.audio

import be.tarsos.dsp.pitch.FastYin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Tier 0 PoC — TarsosDSP 피치(음정) 감지 검증.
 *
 * 검증 대상은 두 가지뿐이다.
 *  1) 알려진 주파수의 사인파를 넣으면 raw Hz 를 맞게 감지하는가 (FastYin.getPitch)
 *  2) 감지한 Hz 를 MIDI 없이 순수 산술로 음이름(음정)으로 변환할 수 있는가
 *
 * 오디오 파일 / 마이크(AudioRecord) 없이 사인파를 즉석 생성해 순수 JVM 에서 돈다.
 */
class PitchDetectionPocTest {

    private val sampleRate = 22050f
    private val bufferSize = 2048

    /** 지정 주파수의 사인파 버퍼 생성 (진폭 1.0) */
    private fun sineWave(freqHz: Float): FloatArray =
        FloatArray(bufferSize) { i -> sin(2.0 * PI * freqHz * i / sampleRate).toFloat() }

    private val 음이름 = arrayOf("도", "도#", "레", "레#", "미", "파", "파#", "솔", "솔#", "라", "라#", "시")

    /**
     * Hz → 음이름 + 옥타브. MIDI 파일과 무관한 순수 매칭.
     * 국제표기(SPN, C4=가온다) 기준. 한국식 옥타브 관례를 쓰려면 octave 에 상수 오프셋만 더하면 된다.
     */
    private fun toNoteName(hz: Float): String {
        val n = (69 + 12f * ln(hz / 440f) / ln(2f)).roundToInt() // A4(440Hz)=69 기준 반음 인덱스
        val name = 음이름[((n % 12) + 12) % 12]
        val octave = n / 12 - 1
        return "${octave}옥타브 $name"
    }

    // --- 1) raw Hz 감지 ---
    @Test
    fun `440Hz 사인파의 피치를 감지한다`() {
        val result = FastYin(sampleRate, bufferSize).getPitch(sineWave(440f))

        assertTrue("유성음으로 판정돼야 함", result.isPitched)
        assertEquals(440f, result.pitch, 5f) // ±5Hz 허용
    }

    // --- 2) 여러 음정 추적 ---
    @Test
    fun `여러 음정을 추적한다`() {
        val yin = FastYin(sampleRate, bufferSize)
        listOf(220f, 330f, 440f, 660f).forEach { hz ->
            assertEquals("$hz Hz 감지 실패", hz, yin.getPitch(sineWave(hz)).pitch, hz * 0.03f) // ±3%
        }
    }

    // --- 3) 감지 Hz → 음이름 변환 (핵심: 음정까지) ---
    @Test
    fun `감지한 주파수를 음이름으로 변환한다`() {
        val yin = FastYin(sampleRate, bufferSize)
        assertEquals("4옥타브 라", toNoteName(yin.getPitch(sineWave(440f)).pitch))    // A4
        assertEquals("4옥타브 도", toNoteName(yin.getPitch(sineWave(261.63f)).pitch))  // 가온다 C4
        assertEquals("5옥타브 도", toNoteName(yin.getPitch(sineWave(523.25f)).pitch))  // C5
    }

    // --- 4) 변환식 단독 검증 (감지기 없이 산술만) ---
    @Test
    fun `주파수-음이름 변환식이 정확하다`() {
        assertEquals("4옥타브 라", toNoteName(440f))
        assertEquals("4옥타브 도", toNoteName(261.63f))
        assertEquals("2옥타브 미", toNoteName(82.41f)) // E2 (기타 6번 개방현)
    }
}
