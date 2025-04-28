package com.kintmin.presentation.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import kotlinx.serialization.Serializable

@Serializable
enum class MainTabItem {
    Search,
    Playlist,
    ;

    fun getLabel() = when (this) {
        Search -> "음원추가"
        Playlist -> "플레이리스트"
    }

    fun getIcon() = when (this) {
        Search -> Icons.Rounded.Search
        Playlist -> Icons.Rounded.VideoLibrary
    }
}
