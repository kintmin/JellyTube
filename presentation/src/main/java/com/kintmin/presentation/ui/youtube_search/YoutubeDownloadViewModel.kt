package com.kintmin.presentation.ui.youtube_search

import androidx.lifecycle.ViewModel
import com.kintmin.presentation.worker.usecase.ExecuteYoutubeDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class YoutubeDownloadViewModel @Inject constructor(
    private val executeYoutubeDownloadUseCase: ExecuteYoutubeDownloadUseCase,
) : ViewModel() {

    fun startDownload(youtubeUrl: String) {
        executeYoutubeDownloadUseCase(youtubeUrl)
    }
}