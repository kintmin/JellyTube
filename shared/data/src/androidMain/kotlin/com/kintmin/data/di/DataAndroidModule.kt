package com.kintmin.data.di

import com.kintmin.data.audio_edit.AudioEditor
import com.kintmin.data.audio_edit.AudioEditorImpl
import com.kintmin.data.data_migration.localDeviceName
import com.kintmin.data.device_status.DeviceStatus
import com.kintmin.data.device_status.DeviceStatusImpl
import com.kintmin.data.file_share.AndroidFileShareServerRepository
import com.kintmin.data.local_datastore.createAndroidPreferencesDataStore
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.database.createAndroidJellyTubeDatabase
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.FileManagerImpl
import com.kintmin.data.network.dataSource.MigrationDataSource
import com.kintmin.data.network.dataSourceImpl.MigrationDataSourceImpl
import com.kintmin.data.translation.LyricsTranslator
import com.kintmin.data.translation.MlKitLyricsTranslator
import com.kintmin.domain.data_migration.repository.MigrationSendServerRepository
import com.kintmin.domain.data_migration.usecase.CloseMigrationSendUseCase
import com.kintmin.domain.data_migration.usecase.FetchMigrationSendStatusUseCase
import com.kintmin.domain.data_migration.usecase.OpenMigrationSendUseCase
import com.kintmin.domain.file_share.repository.PcFileShareServerRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.binds
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

    // 보내는 쪽 서버는 Android 에만 있다. PC 수신과 한 서버를 같이 쓴다.
    single { AndroidFileShareServerRepository(androidContext(), get()) }
        .binds(arrayOf(PcFileShareServerRepository::class, MigrationSendServerRepository::class))
    factoryOf(::OpenMigrationSendUseCase)
    factoryOf(::CloseMigrationSendUseCase)
    factoryOf(::FetchMigrationSendStatusUseCase)
}
