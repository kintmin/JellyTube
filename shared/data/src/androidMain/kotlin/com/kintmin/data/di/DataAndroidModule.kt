package com.kintmin.data.di

import com.kintmin.data.audio_edit.AudioEditor
import com.kintmin.data.audio_edit.AudioEditorImpl
import com.kintmin.data.data_migration.localDeviceName
import com.kintmin.data.device_status.DeviceStatus
import com.kintmin.data.device_status.DeviceStatusImpl
import com.kintmin.data.local_datastore.createAndroidPreferencesDataStore
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.database.createAndroidJellyTubeDatabase
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.FileManagerImpl
import com.kintmin.data.network.dataSource.MigrationDataSource
import com.kintmin.data.network.dataSourceImpl.MigrationDataSourceImpl
import com.kintmin.data.translation.LyricsTranslator
import com.kintmin.data.translation.MlKitLyricsTranslator
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val dataAndroidModule: Module = module {
    single { createAndroidPreferencesDataStore(androidContext()) }
    single<JellyTubeDatabase> { createAndroidJellyTubeDatabase(androidContext()) }
    single<DeviceStatus> { DeviceStatusImpl(androidContext()) }
    single<FileManager> { FileManagerImpl(androidContext()) }
    single<LyricsTranslator> { MlKitLyricsTranslator() }
    single<AudioEditor> { AudioEditorImpl(androidContext()) }
    single<MigrationDataSource> {
        val context = androidContext()
        MigrationDataSourceImpl(get()) { localDeviceName(context) }
    }
}
