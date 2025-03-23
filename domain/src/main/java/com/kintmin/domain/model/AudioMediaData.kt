package com.kintmin.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AudioMediaData(
    val videoId: String,
    val audioFileFullPath: String,
    val imageFileFullPath: String?,
    val title: String,
    val description: String,
): Parcelable