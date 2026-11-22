package com.kintmin.presentation.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.kintmin.domain.app_update.model.AppRelease
import com.kintmin.domain.app_update.usecase.CheckAppUpdateOncePerDayUseCase
import com.kintmin.domain.app_update.usecase.IgnoreAppUpdateUseCase
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelAppUpdateTest {

    private val release = AppRelease(versionName = "2.1.0", downloadUrl = "https://example.com/app.apk")
    private val ignore = mockk<IgnoreAppUpdateUseCase>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // 호스트 JVM 에는 Bundle 구현이 없어 실제 toRoute 는 터진다.
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            any<SavedStateHandle>().toRoute(MainScreenRoute::class, any())
        } returns MainScreenRoute(MainTabItem.Playlist)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
        Dispatchers.resetMain()
    }

    private fun viewModel(result: Result<AppRelease?>) = MainViewModel(
        savedStateHandle = SavedStateHandle(),
        checkAppUpdateOncePerDayUseCase = mockk<CheckAppUpdateOncePerDayUseCase> {
            coEvery { this@mockk(any(), any(), any()) } returns result
        },
        ignoreAppUpdateUseCase = ignore,
    )

    @Test
    fun `새 버전이 있으면 팝업을 띄운다`() {
        val viewModel = viewModel(Result.success(release))

        viewModel.sendIntent(MainIntent.CheckAppUpdate("2.0.0"))

        assertEquals(release, viewModel.uiState.value.availableUpdate)
    }

    @Test
    fun `확인에 실패하면 팝업을 띄우지 않는다`() {
        val viewModel = viewModel(Result.failure(RuntimeException("offline")))

        viewModel.sendIntent(MainIntent.CheckAppUpdate("2.0.0"))

        assertNull(viewModel.uiState.value.availableUpdate)
    }

    @Test
    fun `이번 버전 무시는 그 버전을 기록하고 팝업을 닫는다`() {
        val viewModel = viewModel(Result.success(release))
        viewModel.sendIntent(MainIntent.CheckAppUpdate("2.0.0"))

        viewModel.sendIntent(MainIntent.IgnoreAppUpdate)

        assertNull(viewModel.uiState.value.availableUpdate)
        coVerify { ignore("2.1.0") }
    }

    @Test
    fun `닫기는 무시로 기록하지 않는다`() {
        val viewModel = viewModel(Result.success(release))
        viewModel.sendIntent(MainIntent.CheckAppUpdate("2.0.0"))

        viewModel.sendIntent(MainIntent.DismissAppUpdate)

        assertNull(viewModel.uiState.value.availableUpdate)
        coVerify(exactly = 0) { ignore(any()) }
    }
}
