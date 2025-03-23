package com.kintmin.domain.repository

import com.kintmin.domain.model.AudioMediaData

interface AudioMediaRepository {
    suspend fun getAudioMediaList(): Result<List<AudioMediaData>>
}