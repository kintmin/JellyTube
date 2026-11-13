package com.kintmin.data.di

import com.kintmin.data.audio_edit.AudioEditor
import com.kintmin.data.audio_edit.AudioEditorImpl
import com.kintmin.data.data_migration.localDeviceName
import com.kintmin.data.device_status.DeviceStatus
import com.kintmin.data.device_status.DeviceStatusImpl
import com.kintmin.data.file_share.PcFileShareServerRepositoryImpl
import com.kintmin.data.local_datastore.createIosPreferencesDataStore
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.database.createIosJellyTubeDatabase
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.FileManagerImpl
import com.kintmin.data.network.dataSource.MigrationDataSource
import com.kintmin.data.network.dataSourceImpl.MigrationDataSourceImpl
import com.kintmin.data.python_bridge.IosLyricsTransliterator
import com.kintmin.data.python_bridge.IosPythonExecutorAdapter
import com.kintmin.data.python_bridge.IosPythonExecutorBridge
import com.kintmin.data.python_bridge.LyricsTransliterator
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.translation.IosLyricsTranslator
import com.kintmin.data.translation.IosLyricsTranslatorBridge
import com.kintmin.data.translation.LyricsTranslator
import com.kintmin.domain.file_share.repository.PcFileShareServerRepository
import com.kintmin.domain.file_share.usecase.FetchPcFileShareServerRunningUseCase
import com.kintmin.domain.file_share.usecase.StartPcFileShareServerUseCase
import com.kintmin.domain.file_share.usecase.StopPcFileShareServerUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * `dataAndroidModule` 과 달리 함수인 이유: Python·번역 구현체가 Swift 쪽에 있어 런타임에만 손에
 * 들어온다. Android 는 같은 것을 `androidContext()` 로 받지만 iOS 에는 그 창구가 없다.
 *
 * `AppLog` 는 여기 없다 — 수집기 구현이 Swift 라 조립 모듈(`:ios:shared`)이 맡는다.
 */
fun dataIosModule(
    pythonExecutorBridge: IosPythonExecutorBridge,
    lyricsTranslatorBridge: IosLyricsTranslatorBridge,
): Module = module {
    single { createIosPreferencesDataStore() }
    single<JellyTubeDatabase> { createIosJellyTubeDatabase() }
    single<DeviceStatus> { DeviceStatusImpl() }
    single<AudioEditor> { AudioEditorImpl() }
    single<FileManager> { FileManagerImpl() }
    single<MigrationDataSource> { MigrationDataSourceImpl(get(), ::localDeviceName) }
    single<PythonExecutor> { IosPythonExecutorAdapter(pythonExecutorBridge) }
    // 브릿지 자체도 꺼낼 수 있어야 한다 — 끝난 전송 목록처럼 PythonExecutor 밖의 일이 있다.
    single<IosPythonExecutorBridge> { pythonExecutorBridge }
    single<LyricsTranslator> { IosLyricsTranslator(lyricsTranslatorBridge) }
    single<LyricsTransliterator> { IosLyricsTransliterator(pythonExecutorBridge) }

    // Android 는 PC 수신 서버를 android:app 의 포그라운드 서비스가 직접 띄워 이 저장소가 없다.
    single<PcFileShareServerRepository> { PcFileShareServerRepositoryImpl(get()) }
    factoryOf(::StartPcFileShareServerUseCase)
    factoryOf(::StopPcFileShareServerUseCase)
    factoryOf(::FetchPcFileShareServerRunningUseCase)
}
