package com.kintmin.presentation.ui.youtube_search

import androidx.lifecycle.ViewModel
import com.kintmin.platformruntime.ExecuteYoutubeDownload
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class YoutubeDownloadViewModel @Inject constructor(
    private val executeYoutubeDownload: ExecuteYoutubeDownload,
) : ViewModel() {

    fun startDownload(youtubeUrl: String) {
        executeYoutubeDownload(youtubeUrl)
    }
}