package com.kintmin.jellytube.di

import com.kintmin.data.python_bridge.LyricsTransliterator
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.jellytube.MainViewModel
import com.kintmin.jellytube.log_impl.AppLogImpl
import com.kintmin.jellytube.python_bridge_impl.LyricsTransliteratorImpl
import com.kintmin.jellytube.python_bridge_impl.PythonExecutorImpl
import com.kintmin.log.AppLog
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule: Module = module {
    viewModelOf(::MainViewModel)
    single<PythonExecutor> { PythonExecutorImpl(androidContext()) }
    single<LyricsTransliterator> { LyricsTransliteratorImpl(androidContext()) }
}

val logModule: Module = module {
    singleOf(::AppLogImpl) bind AppLog::class
}