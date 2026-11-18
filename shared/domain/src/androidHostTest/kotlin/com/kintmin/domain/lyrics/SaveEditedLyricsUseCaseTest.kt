package com.kintmin.domain.lyrics

import com.kintmin.domain.lyrics.model.EditedLyricsRow
import com.kintmin.domain.lyrics.usecase.ApplyLyricsToAudioMediaUseCase
import com.kintmin.domain.lyrics.usecase.SaveEditedLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SerializeLyricsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SaveEditedLyricsUseCaseTest {

    private val apply = mockk<ApplyLyricsToAudioMediaUseCase> {
        coEvery { this@mockk(any(), any(), any(), any(), any(), any()) } returns Result.success("path")
    }
    private val useCase = SaveEditedLyricsUseCase(SerializeLyricsUseCase(), apply)
    private val rows = listOf(EditedLyricsRow(timeMs = 1_000, text = "원문", translation = "번역", transliteration = "음차"))

    @Test
    fun `번역·음차가 없으면 원문만 저장한다`() = runTest {
        useCase(1, rows, hasTranslation = false, hasTransliteration = false, previousLyricFileFullPath = "old")

        coVerify { apply(1, null, match { "원문" in it }, null, null, "old") }
    }

    @Test
    fun `번역·음차는 원문과 같은 타임코드로 저장한다`() = runTest {
        useCase(1, rows, hasTranslation = true, hasTransliteration = true, previousLyricFileFullPath = null)

        coVerify {
            apply(
                1,
                null,
                match { it.startsWith("[00:01") && "원문" in it },
                match { it.startsWith("[00:01") && "번역" in it },
                match { it.startsWith("[00:01") && "음차" in it },
                null,
            )
        }
    }
}
