package com.kintmin.data.file_share

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.kintmin.data.data_migration.localDeviceName
import com.kintmin.fileshare.FileShareConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** 서버가 떠 있는 동안 [start] 에 준 서비스 타입들을 광고한다. */
internal class NsdAdvertiser(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val registrations = mutableMapOf<String, NsdManager.RegistrationListener>()
    private var advertiseJob: Job? = null
    private var targets: Set<String> = emptySet()
    private var retryCount = 0
    private var isClosed = true

    @Synchronized
    fun start(serviceTypes: Flow<Set<String>>) {
        if (advertiseJob?.isActive == true) return
        isClosed = false
        advertiseJob = scope.launch {
            serviceTypes.distinctUntilChanged().collect(::sync)
        }
    }

    @Synchronized
    fun stop() {
        advertiseJob?.cancel()
        advertiseJob = null
        isClosed = true
        sync(emptySet())
    }

    // 수집이 취소된 뒤에도 이미 꺼낸 값으로 한 번 더 불릴 수 있다. 닫힌 뒤에는 다시 등록하지 않는다.
    @Synchronized
    private fun sync(serviceTypes: Set<String>) {
        if (serviceTypes != targets) retryCount = 0
        targets = serviceTypes
        val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val wanted = if (isClosed) emptySet() else serviceTypes
        (registrations.keys - wanted).forEach { serviceType ->
            registrations.remove(serviceType)?.let { runCatching { manager.unregisterService(it) } }
        }
        (wanted - registrations.keys).forEach { serviceType ->
            val listener = object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) = onRegistrationFailed(serviceType, this)
                override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceRegistered(info: NsdServiceInfo) {}
                override fun onServiceUnregistered(info: NsdServiceInfo) {}
            }
            runCatching { manager.registerService(serviceInfo(serviceType), NsdManager.PROTOCOL_DNS_SD, listener) }
                .onSuccess { registrations[serviceType] = listener }
                .onFailure { retry() }
        }
    }

    // 실패한 등록을 남겨 두면 등록된 줄 알고 다시 시도하지 않아 받는 기기가 찾지 못한다.
    @Synchronized
    private fun onRegistrationFailed(serviceType: String, listener: NsdManager.RegistrationListener) {
        if (registrations[serviceType] !== listener) return
        registrations.remove(serviceType)
        retry()
    }

    // 광고 대상이 그대로면 다시 맞출 계기가 없다. 직접 다시 맞춘다.
    @Synchronized
    private fun retry() {
        if (retryCount >= MAX_RETRIES) return
        retryCount++
        scope.launch {
            delay(RETRY_DELAY_MS)
            sync(targets)
        }
    }

    private fun serviceInfo(serviceType: String) = NsdServiceInfo().apply {
        this.serviceType = serviceType
        port = FileShareConstants.DEFAULT_PORT
        if (serviceType == FileShareConstants.MIGRATION_NSD_SERVICE_TYPE) {
            serviceName = toDnsSdInstanceName(localDeviceName(context))
            setAttribute(FileShareConstants.MIGRATION_TXT_FORMAT_VERSION, FileShareConstants.MIGRATION_FORMAT_VERSION.toString())
        } else {
            serviceName = FileShareConstants.NSD_SERVICE_NAME
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 2_000L
    }
}
