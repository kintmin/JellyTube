package com.kintmin.data.di

import com.kintmin.data.audio_edit.AudioEditor
import com.kintmin.data.audio_edit.AudioEditorImpl
import com.kintmin.data.device_status.DeviceStatus
import com.kintmin.data.device_status.DeviceStatusImpl
import com.kintmin.data.local_datastore.createIosPreferencesDataStore
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.database.createIosJellyTubeDatabase
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.FileManagerImpl
import com.kintmin.data.python_bridge.IosLyricsTransliterator
import com.kintmin.data.python_bridge.IosPythonExecutorAdapter
import com.kintmin.data.python_bridge.IosPythonExecutorBridge
import com.kintmin.data.python_bridge.LyricsTransliterator
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.translation.IosLyricsTranslator
import com.kintmin.data.translation.LyricsTranslator
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * `dataAndroidModule` 과 달리 함수인 이유: Python 구현체가 Swift 쪽에 있어 런타임에만 손에
 * 들어온다. Android 는 같은 것을 `androidContext()` 로 받지만 iOS 에는 그 창구가 없다.
 *
 * `AppLog` 는 여기 없다 — 수집기 구현이 Swift 라 조립 모듈(`:ios:shared`)이 맡는다.
 */
fun dataIosModule(pythonExecutorBridge: IosPythonExecutorBridge): Module = module {
    single { createIosPreferencesDataStore() }
    single<JellyTubeDatabase> { createIosJellyTubeDatabase() }
    single<DeviceStatus> { DeviceStatusImpl() }
    single<AudioEditor> { AudioEditorImpl() }
    single<FileManager> { FileManagerImpl() }
    single<PythonExecutor> { IosPythonExecutorAdapter(pythonExecutorBridge) }
    // 브릿지 자체도 꺼낼 수 있어야 한다 — 끝난 전송 목록처럼 PythonExecutor 밖의 일이 있다.
    single<IosPythonExecutorBridge> { pythonExecutorBridge }
    single<LyricsTranslator> { IosLyricsTranslator() }
    single<LyricsTransliterator> { IosLyricsTransliterator(pythonExecutorBridge) }
}
