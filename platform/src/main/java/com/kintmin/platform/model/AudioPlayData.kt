package com.kintmin.platform.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AudioPlayData(
    val id: Int,
    val mediaName: String,
    val description: String,
    val artist: String,
    val audioFileFullPath: String,
    val imageFileFullPath: String?,
) : Parcelable
