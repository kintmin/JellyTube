package com.kintmin.data.di

import com.kintmin.data.local_datastore.createIosPreferencesDataStore
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.database.createIosJellyTubeDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val dataIosModule: Module = module {
    single { createIosPreferencesDataStore() }
    single<JellyTubeDatabase> { createIosJellyTubeDatabase() }
}
