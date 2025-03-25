package com.kintmin.localfile.di

import android.content.Context
import com.kintmin.localfile.FileManager
import com.kintmin.localfile.FileManagerImpl
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