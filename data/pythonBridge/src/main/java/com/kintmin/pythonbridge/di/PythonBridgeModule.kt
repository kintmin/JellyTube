package com.kintmin.pythonbridge.di

import android.content.Context
import com.kintmin.pythonbridge.PythonExecutor
import com.kintmin.pythonbridge.PythonExecutorImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object PythonBridgeModule {
    @Provides
    fun providePythonExecutor(
        @ApplicationContext context: Context,
    ): PythonExecutor {
        return PythonExecutorImpl(context)
    }
}