package com.kintmin.desktop.update

import com.kintmin.desktop.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainViewModelUpdateTest {

    private val release = DesktopRelease(versionName = "1.1.0", downloadUrl = "https://dl/msi")
    private val ignored = mutableListOf<String>()

    private fun viewModel() = MainViewModel(
        discover = { null },
        findUpdate = { release },
        ignoreUpdate = { ignored += it },
    )

    @Test
    fun `켜면 새 버전을 알린다`() = runTest {
        val viewModel = viewModel()

        assertEquals(release, viewModel.uiState.first { it.availableUpdate != null }.availableUpdate)
        viewModel.onDispose()
    }

    @Test
    fun `이번 버전 무시는 그 버전을 기록하고 닫는다`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.availableUpdate != null }

        viewModel.onIgnoreUpdate()

        assertNull(viewModel.uiState.value.availableUpdate)
        assertEquals(listOf("1.1.0"), ignored)
        viewModel.onDispose()
    }

    @Test
    fun `닫기는 무시로 기록하지 않는다`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.availableUpdate != null }

        viewModel.onDismissUpdate()

        assertNull(viewModel.uiState.value.availableUpdate)
        assertEquals(emptyList(), ignored)
        viewModel.onDispose()
    }
}
