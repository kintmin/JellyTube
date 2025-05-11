package com.kintmin.presentation.ui.main.youtube_search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.kintmin.platform.ExecuteYoutubeDownload
import com.kintmin.presentation.ui.main.navigation.MainScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class YoutubeDownloadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val executeYoutubeDownload: ExecuteYoutubeDownload,
) : ViewModel() {

    private val _currentUrl = MutableStateFlow(
        savedStateHandle.toRoute<MainScreenRoute>().searchUrl ?: "https://www.youtube.com/"
    )
    val currentUrl = _currentUrl.asStateFlow()

    fun sendIntent(intent: YoutubeDownloadIntent) {
        when(intent) {
            is YoutubeDownloadIntent.OnClickDownload -> startDownload(intent.url)
            is YoutubeDownloadIntent.OnChangeUrl -> _currentUrl.update { intent.url }
        }
    }

    private fun startDownload(youtubeUrl: String) {
        executeYoutubeDownload(youtubeUrl)
    }
}