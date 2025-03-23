package com.kintmin.presentation.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTabItem(val label: String, val icon: ImageVector) {
    Search("음원찾기", Icons.Default.Add),
    Play("음원감상", Icons.Default.PlayArrow)
}