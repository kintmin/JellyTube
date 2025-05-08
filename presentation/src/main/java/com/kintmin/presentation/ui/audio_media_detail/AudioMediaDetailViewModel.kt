package com.kintmin.presentation.ui.audio_media_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.kintmin.presentation.ui.playlist_detail.navigation.PlaylistDetailScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AudioMediaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val playlistId = savedStateHandle.toRoute<PlaylistDetailScreenRoute>().playlistId
    private val audioMediaId = savedStateHandle.toRoute<PlaylistDetailScreenRoute>().playlistId

}