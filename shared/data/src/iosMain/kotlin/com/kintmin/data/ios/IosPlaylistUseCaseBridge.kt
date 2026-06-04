package com.kintmin.data.ios

import com.kintmin.data.di.dataCommonModule
import com.kintmin.data.di.dataIosModule
import com.kintmin.data.local_file.FileManager
import com.kintmin.data.local_file.FileManagerImpl
import com.kintmin.data.python_bridge.IosPythonExecutorAdapter
import com.kintmin.data.python_bridge.IosPythonExecutorBridge
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.AddNewPlaylistUseCase
import com.kintmin.domain.playlist.usecase.DeletePlaylistUseCase
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
import com.kintmin.log.AppLog
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.dsl.module

class IosFetchAllPlaylistFlowUseCaseBridge : KoinComponent {

    operator fun invoke(): Flow<List<Playlist>> = get<FetchAllPlaylistFlowUseCase>()()
}

class IosAddNewPlaylistUseCaseBridge : KoinComponent {

    suspend operator fun invoke(title: String): Int = get<AddNewPlaylistUseCase>()(title).getOrThrow()
}

class IosDeletePlaylistUseCaseBridge : KoinComponent {

    suspend operator fun invoke(id: Int) {
        get<DeletePlaylistUseCase>()(id)
    }
}

fun createIosFetchAllPlaylistFlowUseCaseBridge(): IosFetchAllPlaylistFlowUseCaseBridge =
    IosFetchAllPlaylistFlowUseCaseBridge()

fun createIosAddNewPlaylistUseCaseBridge(): IosAddNewPlaylistUseCaseBridge =
    IosAddNewPlaylistUseCaseBridge()

fun createIosDeletePlaylistUseCaseBridge(): IosDeletePlaylistUseCaseBridge =
    IosDeletePlaylistUseCaseBridge()

fun initIosKoin(pythonExecutorBridge: IosPythonExecutorBridge) {
    startKoin {
        modules(
            dataCommonModule,
            dataIosModule,
            module {
                single<FileManager> { FileManagerImpl() }
                single<AppLog> { IosAppLog() }
                single<PythonExecutor> { IosPythonExecutorAdapter(pythonExecutorBridge) }
            },
        )
    }
}
