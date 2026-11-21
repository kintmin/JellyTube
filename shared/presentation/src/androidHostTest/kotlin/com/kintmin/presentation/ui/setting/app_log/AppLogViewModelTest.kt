package com.kintmin.presentation.ui.setting.app_log

import com.kintmin.domain.app_log.usecase.FetchAppLogDateListUseCase
import com.kintmin.domain.app_log.usecase.FetchAppLogLineListUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 날짜를 읽는 동안 화면이 빈 상태나 진행 표시로 바뀌었다 돌아오며 깜박이지 않는지 본다. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppLogViewModelTest {

    private val firstDateLines = CompletableDeferred<List<String>>()
    private val secondDateLines = CompletableDeferred<List<String>>()

    private fun viewModel() = AppLogViewModel(
        fetchAppLogDateListUseCase = mockk<FetchAppLogDateListUseCase> {
            coEvery { this@mockk.invoke() } returns Result.success(listOf("2026-11-21", "2026-11-20"))
        },
        fetchAppLogLineListUseCase = mockk<FetchAppLogLineListUseCase> {
            coEvery { this@mockk.invoke("2026-11-21") } coAnswers { Result.success(firstDateLines.await()) }
            coEvery { this@mockk.invoke("2026-11-20") } coAnswers { Result.success(secondDateLines.await()) }
        },
        fetchAppLogFileFullPathsUseCase = mockk(relaxed = true),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `첫 날의 줄을 읽는 동안에는 빈 상태가 아니라 읽는 중이다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(AppLogIntent.OnInit)

        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.logLineList.isEmpty())
    }

    @Test
    fun `날을 바꾸면 새 날을 다 읽을 때까지 이전 날의 줄을 들고 있다`() {
        val viewModel = viewModel()
        viewModel.sendIntent(AppLogIntent.OnInit)
        firstDateLines.complete(listOf("[2026-11-21 09:00:00.000] [DEBUG] 첫날"))

        viewModel.sendIntent(AppLogIntent.OnClickLogDate("2026-11-20"))

        assertEquals("2026-11-20", viewModel.uiState.value.selectedLogDate)
        assertEquals(listOf("첫날"), viewModel.uiState.value.logLineList.map { it.message })

        secondDateLines.complete(listOf("[2026-11-20 09:00:00.000] [DEBUG] 둘째날"))

        assertEquals(listOf("둘째날"), viewModel.uiState.value.logLineList.map { it.message })
    }
}
