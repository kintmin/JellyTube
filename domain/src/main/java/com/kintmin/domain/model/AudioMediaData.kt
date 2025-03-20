package com.kintmin.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AudioMediaData(
    val videoId: String,
    val audioFilePath: String,
    val imageFilePath: String?,
    val title: String,
    val description: String,
): Parcelable