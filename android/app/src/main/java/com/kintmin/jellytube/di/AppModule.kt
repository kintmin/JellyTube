package com.kintmin.jellytube.di

import com.kintmin.jellytube.MainViewModel
import com.kintmin.jellytube.log_impl.AppLogImpl
import com.kintmin.log.AppLog
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule: Module = module {
    viewModelOf(::MainViewModel)
}

val logModule: Module = module {
    singleOf(::AppLogImpl) bind AppLog::class
}