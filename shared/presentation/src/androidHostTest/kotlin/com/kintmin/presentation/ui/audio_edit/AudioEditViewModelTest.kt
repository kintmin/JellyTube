package com.kintmin.presentation.ui.audio_edit

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.kintmin.domain.app_setting.usecase.FetchIsAudioEditGuideShownFlowUseCase
import com.kintmin.domain.app_setting.usecase.MarkAudioEditGuideShownUseCase
import com.kintmin.domain.audio_edit.model.AudioEditClip
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.usecase.FetchAudioEditTimelineUseCase
import com.kintmin.domain.audio_edit.usecase.MeasureAudioDurationUseCase
import com.kintmin.domain.audio_edit.usecase.ObserveAudioEditSavingUseCase
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.presentation.ui.audio_edit.navigation.AudioEditScreenRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioEditViewModelTest {

    private val aggregate = mockk<PlaylistTrackAggregate>(relaxed = true) {
        every { audioMedia.name } returns "제목3"
        // 비워 두면 파형 추출을 건너뛴다
        every { audioMedia.audioFileFullPath } returns ""
        every { audioMedia.editedAudioFileFullPath } returns null
        every { audioMedia.lyricFileFullPath } returns null
    }

    private val isGuideShown = MutableStateFlow(false)

    private val markGuideShown = mockk<MarkAudioEditGuideShownUseCase> {
        coEvery { this@mockk.invoke() } coAnswers {
            isGuideShown.value = true
            Result.success(Unit)
        }
    }

    private fun viewModel(savedTimeline: AudioEditTimeline? = null) = AudioEditViewModel(
        savedStateHandle = SavedStateHandle(),
        fetchAudioMediaDetailFlowUseCase = mockk<FetchAudioMediaDetailFlowUseCase> {
            every { this@mockk.invoke(any()) } returns flowOf(listOf(aggregate))
        },
        extractAudioWaveformUseCase = mockk(relaxed = true),
        fetchAudioEditTimelineUseCase = mockk<FetchAudioEditTimelineUseCase> {
            coEvery { this@mockk.invoke(any()) } returns Result.success(savedTimeline)
        },
        measureAudioDurationUseCase = mockk<MeasureAudioDurationUseCase> {
            coEvery { this@mockk.invoke(any()) } returns Result.success(ORIGIN_DURATION_MS)
        },
        observeAudioEditSavingUseCase = mockk<ObserveAudioEditSavingUseCase> {
            every { this@mockk.invoke(any()) } returns flowOf(false)
        },
        reserveAudioEditSaveUseCase = mockk(relaxed = true),
        releaseAudioEditSaveUseCase = mockk(relaxed = true),
        executeAudioEditSave = mockk(relaxed = true),
        audioPreviewPlayer = mockk(relaxed = true),
        fetchIsAudioEditGuideShownFlowUseCase = mockk<FetchIsAudioEditGuideShownFlowUseCase> {
            every { this@mockk.invoke() } returns isGuideShown
        },
        markAudioEditGuideShownUseCase = markGuideShown,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // 라우트 역직렬화가 android.os.Bundle 을 거쳐 호스트 테스트의 스텁에서 터진다.
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            any<SavedStateHandle>().toRoute(AudioEditScreenRoute::class, any())
        } returns AudioEditScreenRoute(3)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
    }

    @Test
    fun `처음 기준선은 결과물 가운데에 선다`() {
        val state = viewModel(savedTimeline = null).uiState.value

        assertEquals(ORIGIN_DURATION_MS / 2, state.baselineMs)
    }

    @Test
    fun `저장된 편집 상태로 시작하면 그 결과물의 가운데에 선다`() {
        val saved = AudioEditTimeline(
            originDurationMs = ORIGIN_DURATION_MS,
            clips = listOf(AudioEditClip(sourceStartMs = 0, sourceEndMs = 60_000, positionMs = 0)),
            endMs = 60_000,
        )

        val state = viewModel(savedTimeline = saved).uiState.value

        assertEquals(30_000L, state.baselineMs)
    }

    @Test
    fun `안내를 본 적이 없으면 띄우고, 닫으면 기록해 다시 띄우지 않는다`() {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.isGuideVisible)

        viewModel.sendIntent(AudioEditIntent.OnDismissGuide)

        assertFalse(viewModel.uiState.value.isGuideVisible)
        coVerify(exactly = 1) { markGuideShown() }
        assertFalse(viewModel().uiState.value.isGuideVisible)
    }

    @Test
    fun `안내 기록에 실패해도 지금은 닫는다`() {
        coEvery { markGuideShown() } returns Result.failure(IllegalStateException())
        val viewModel = viewModel()

        viewModel.sendIntent(AudioEditIntent.OnDismissGuide)

        assertFalse(viewModel.uiState.value.isGuideVisible)
    }

    private companion object {
        const val ORIGIN_DURATION_MS = 200_000L
    }
}
