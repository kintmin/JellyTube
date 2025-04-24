package com.kintmin.presentation.ui.youtube_search

import androidx.lifecycle.ViewModel
import com.kintmin.platform.ExecuteYoutubeDownload
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class YoutubeDownloadViewModel @Inject constructor(
    private val executeYoutubeDownload: ExecuteYoutubeDownload,
) : ViewModel() {

    fun sendIntent(intent: YoutubeDownloadIntent) {
        when(intent) {
            is YoutubeDownloadIntent.OnClickDownload -> startDownload(intent.url)
        }
    }

    private fun startDownload(youtubeUrl: String) {
        executeYoutubeDownload(youtubeUrl)
    }
}