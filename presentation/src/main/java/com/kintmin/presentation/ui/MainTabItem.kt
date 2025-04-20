package com.kintmin.presentation.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import kotlinx.serialization.Serializable

@Serializable
enum class MainTabItem {
    Search,
    Play,
    ;

    fun getLabel() = when (this) {
        Search -> "음원찾기"
        Play -> "음원감상"
    }

    fun getIcon() = when (this) {
        Search -> Icons.Default.Add
        Play -> Icons.Default.PlayArrow
    }
}
