package com.kintmin.data.di

import com.kintmin.data.audio_edit.AudioEditor
import com.kintmin.data.audio_edit.AudioEditorImpl
import com.kintmin.data.device_status.DeviceStatus
import com.kintmin.data.device_status.DeviceStatusImpl
import com.kintmin.data.ios.IosAppLog
import com.kintmin.data.local_datastore.createIosPreferencesDataStore
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.database.createIosJellyTubeDatabase
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.FileManagerImpl
import com.kintmin.data.python_bridge.IosPythonExecutorAdapter
import com.kintmin.data.python_bridge.IosPythonExecutorBridge
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.log.AppLog
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * `dataAndroidModule` 과 달리 함수인 이유: Python 구현체가 Swift 쪽에 있어 **런타임에만**
 * 손에 들어온다. Android 는 같은 것을 `androidContext()` 로 받지만 iOS 에는 그 창구가 없다.
 *
 * 이 바인딩들이 함수 파라미터를 타고 들어오는 대신 조립 모듈(`:ios:shared`)에서 직접
 * 등록되지 않는 것은 [FileManagerImpl] · [IosAppLog] · [IosPythonExecutorAdapter] 가
 * 이 모듈의 `internal` 이기 때문이다. 공개로 넓히는 것보다 파라미터 하나가 싸다.
 */
fun dataIosModule(pythonExecutorBridge: IosPythonExecutorBridge): Module = module {
    single { createIosPreferencesDataStore() }
    single<JellyTubeDatabase> { createIosJellyTubeDatabase() }
    single<DeviceStatus> { DeviceStatusImpl() }
    single<AudioEditor> { AudioEditorImpl() }
    single<FileManager> { FileManagerImpl() }
    single<AppLog> { IosAppLog() }
    single<PythonExecutor> { IosPythonExecutorAdapter(pythonExecutorBridge) }
}
