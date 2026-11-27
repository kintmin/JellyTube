package com.kintmin.domain

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.usecase.RemapLyricsToAudioEditUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemapLyricsToAudioEditUseCaseTest {

    private val useCase = RemapLyricsToAudioEditUseCase()

    private val lines = listOf(
        LyricsLine(timeMs = 0L, text = "첫 줄"),
        LyricsLine(timeMs = 10_000L, text = "둘째 줄"),
        LyricsLine(timeMs = 20_000L, text = "셋째 줄"),
        LyricsLine(timeMs = 30_000L, text = "넷째 줄"),
    )

    @Test
    fun `앞을 자르면 남은 줄이 자른 만큼 당겨진다`() {
        val remapped = useCase(lines, listOf(AudioEditPart.Audio(startMs = 10_000, endMs = 40_000)))

        assertEquals(listOf(1, 2, 3), remapped.map { it.sourceIndex })
        assertEquals(listOf(0L, 10_000L, 20_000L), remapped.map { it.timeMs })
    }

    @Test
    fun `잘려 나간 구간의 줄은 사라진다`() {
        val remapped = useCase(lines, listOf(AudioEditPart.Audio(startMs = 0, endMs = 15_000)))

        assertEquals(listOf(0, 1), remapped.map { it.sourceIndex })
    }

    @Test
    fun `조각이 줄 중간에서 시작하면 그 줄을 조각 머리에 붙인다`() {
        val remapped = useCase(lines, listOf(AudioEditPart.Audio(startMs = 15_000, endMs = 40_000)))

        assertEquals(listOf(1, 2, 3), remapped.map { it.sourceIndex })
        // 15초에 울리고 있던 둘째 줄이 0초로 오고, 셋째 줄은 5초 뒤에 온다.
        assertEquals(listOf(0L, 5_000L, 15_000L), remapped.map { it.timeMs })
    }

    @Test
    fun `조각 머리와 줄 시작이 맞으면 같은 줄을 두 번 넣지 않는다`() {
        val remapped = useCase(lines, listOf(AudioEditPart.Audio(startMs = 20_000, endMs = 40_000)))

        assertEquals(listOf(2, 3), remapped.map { it.sourceIndex })
        assertEquals(listOf(0L, 10_000L), remapped.map { it.timeMs })
    }

    @Test
    fun `이어 붙인 두 조각의 이음매에서 같은 줄이 연달아 나오지 않는다`() {
        val remapped = useCase(
            lines,
            listOf(
                AudioEditPart.Audio(startMs = 0, endMs = 5_000),
                AudioEditPart.Audio(startMs = 5_000, endMs = 15_000),
            ),
        )

        assertEquals(listOf(0, 1), remapped.map { it.sourceIndex })
        assertEquals(listOf(0L, 10_000L), remapped.map { it.timeMs })
    }

    @Test
    fun `무음은 자리만 밀고 줄을 만들지 않는다`() {
        val remapped = useCase(
            lines,
            listOf(
                AudioEditPart.Silence(durationMs = 3_000),
                AudioEditPart.Audio(startMs = 20_000, endMs = 40_000),
            ),
        )

        assertEquals(listOf(2, 3), remapped.map { it.sourceIndex })
        assertEquals(listOf(3_000L, 13_000L), remapped.map { it.timeMs })
    }

    @Test
    fun `같은 구간을 두 번 놓으면 그 줄도 두 번 나온다`() {
        val remapped = useCase(
            lines,
            listOf(
                AudioEditPart.Audio(startMs = 20_000, endMs = 25_000),
                AudioEditPart.Audio(startMs = 20_000, endMs = 25_000),
            ),
        )

        assertEquals(listOf(2, 2), remapped.map { it.sourceIndex })
        assertEquals(listOf(0L, 5_000L), remapped.map { it.timeMs })
    }

    @Test
    fun `소리 조각이 없으면 가사도 없다`() {
        assertTrue(useCase(lines, listOf(AudioEditPart.Silence(durationMs = 5_000))).isEmpty())
    }

    @Test
    fun `옮긴 결과는 시간 오름차순이다`() {
        val remapped = useCase(
            lines,
            listOf(
                AudioEditPart.Audio(startMs = 25_000, endMs = 40_000),
                AudioEditPart.Silence(durationMs = 2_000),
                AudioEditPart.Audio(startMs = 5_000, endMs = 15_000),
            ),
        )

        assertEquals(remapped.map { it.timeMs }.sorted(), remapped.map { it.timeMs })
    }
}
