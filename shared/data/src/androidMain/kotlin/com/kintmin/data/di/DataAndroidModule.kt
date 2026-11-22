package com.kintmin.data.di

import com.kintmin.data.audio_edit.AudioEditor
import com.kintmin.data.audio_edit.AudioEditorImpl
import com.kintmin.data.backup.BackupFolderAccess
import com.kintmin.data.backup.DownloadsBackupFolderAccess
import com.kintmin.data.data_migration.localDeviceName
import com.kintmin.data.data_migration.toMigrationInstanceName
import com.kintmin.data.device_status.DeviceStatus
import com.kintmin.data.device_status.DeviceStatusImpl
import com.kintmin.data.file_share.FileShareServerRepository
import com.kintmin.data.file_share.KeepAliveByForegroundService
import com.kintmin.data.file_share.NsdAdvertiser
import com.kintmin.data.local_datastore.createAndroidPreferencesDataStore
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.database.createAndroidJellyTubeDatabase
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.FileManagerImpl
import com.kintmin.data.network.createLocalNetworkHttpClient
import com.kintmin.data.network.data_source.MigrationDataSource
import com.kintmin.data.network.data_source_impl.MigrationDataSourceImpl
import com.kintmin.data.translation.LyricsTranslator
import com.kintmin.data.translation.MlKitLyricsTranslator
import com.kintmin.domain.data_migration.repository.MigrationSendServerRepository
import com.kintmin.domain.file_share.repository.PcFileShareServerRepository
import com.kintmin.domain.user.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

val dataAndroidModule: Module = module {
    single { createAndroidPreferencesDataStore(androidContext()) }
    single<JellyTubeDatabase> { createAndroidJellyTubeDatabase(androidContext()) }
    single<DeviceStatus> { DeviceStatusImpl(androidContext()) }
    single<FileManager> { FileManagerImpl(androidContext()) }
    single<BackupFolderAccess> { DownloadsBackupFolderAccess(androidContext()) }
    single<LyricsTranslator> { MlKitLyricsTranslator() }
    single<AudioEditor> { AudioEditorImpl(androidContext()) }
    single<MigrationDataSource> {
        val context = androidContext()
        MigrationDataSourceImpl(createLocalNetworkHttpClient()) { localDeviceName(context) }
    }

    // PC 수신과 한 서버를 같이 쓴다.
    single {
        val context = androidContext()
        val userRepository = get<UserRepository>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        FileShareServerRepository(
            scope = scope,
            appLog = get(),
            uploadTempDir = context.cacheDir.path,
            receiveKeepAlive = KeepAliveByForegroundService,
            advertiser = NsdAdvertiser(context, scope, get()) {
                toMigrationInstanceName(localDeviceName(context), userRepository.getUserId().getOrNull())
            },
        )
    }
        .binds(arrayOf(PcFileShareServerRepository::class, MigrationSendServerRepository::class))
}
