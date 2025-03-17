package com.kintmin.ytmusicbox.service

import com.kintmin.ytmusicbox.data.local.LocalFileDataSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServiceModule {
    fun localFileDataSource(): LocalFileDataSource
}