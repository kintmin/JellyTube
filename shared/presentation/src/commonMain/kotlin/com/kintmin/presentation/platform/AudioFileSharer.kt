package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable

/** 시트를 띄우지 못하면 false. */
fun interface AudioFileSharer {

    fun share(fileFullPath: String, title: String, artist: String): Boolean
}

@Composable
expect fun rememberAudioFileSharer(): AudioFileSharer
