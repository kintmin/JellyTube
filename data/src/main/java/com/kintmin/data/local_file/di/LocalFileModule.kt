package com.kintmin.data.local_file.di

import android.content.Context
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.FileManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalFileModule {
    @Provides
    @Singleton
    fun provideFileManager(
        @ApplicationContext context: Context,
    ): FileManager {
        return FileManagerImpl(context)
    }
}