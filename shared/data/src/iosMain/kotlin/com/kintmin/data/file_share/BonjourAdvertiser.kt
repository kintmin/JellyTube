package com.kintmin.data.file_share

import com.kintmin.fileshare.FileShareConstants
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import kotlinx.cinterop.ObjCSignatureOverride
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import platform.Foundation.NSNetService
import platform.Foundation.NSNetServiceDelegateProtocol
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.darwin.NSObject

/** NSObject 를 상속한 등록은 Kotlin 인터페이스를 함께 구현할 수 없어 두 동작만 묶는다. */
internal class AdvertisementRegistration(
    val publish: () -> Unit,
    val stop: () -> Unit,
)

/** NSNetService 는 만든 스레드의 런루프에서 광고한다. [scope] 는 메인 디스패처여야 한다. */
internal class BonjourAdvertiser(
    private val scope: CoroutineScope,
    private val resolveMigrationInstanceName: suspend () -> String,
    private val register: (serviceType: String, advertised: AdvertisedService) -> AdvertisementRegistration,
) : ServiceAdvertiser {

    private val registrations = mutableMapOf<String, AdvertisementRegistration>()
    private var advertiseJob: Job? = null
    private var migrationInstanceName = ""
    @Volatile
    private var isClosed = true

    override fun start(serviceTypes: Flow<Set<String>>) {
        if (advertiseJob?.isActive == true) return
        isClosed = false
        advertiseJob = scope.launch {
            migrationInstanceName = resolveMigrationInstanceName()
            serviceTypes.distinctUntilChanged().collect(::sync)
        }
    }

    override fun stop() {
        advertiseJob?.cancel()
        advertiseJob = null
        isClosed = true
        scope.launch { sync(emptySet()) }
    }

    override fun refresh() {
        scope.launch { registrations.values.forEach { it.publish() } }
    }

    private fun sync(serviceTypes: Set<String>) {
        val wanted = if (isClosed) emptySet() else serviceTypes
        (registrations.keys - wanted).forEach { registrations.remove(it)?.stop?.invoke() }
        (wanted - registrations.keys).forEach { serviceType ->
            registrations[serviceType] = register(serviceType, advertisedServiceOf(serviceType, migrationInstanceName))
                .also { it.publish() }
        }
    }
}

/** 등록이 실패해도 서버는 멀쩡해 보여 상대가 찾지 못한다. 몇 번 다시 등록한다. */
internal class BonjourRegistration(
    private val scope: CoroutineScope,
    private val appLog: AppLog,
    private val serviceType: String,
    private val advertised: AdvertisedService,
) : NSObject(), NSNetServiceDelegateProtocol {
    private var service: NSNetService? = null
    private var retryCount = 0
    private var retryJob: Job? = null

    fun publish() {
        retryCount = 0
        register()
    }

    fun stop() {
        retryJob?.cancel()
        unregister()
    }

    private fun register() {
        unregister()
        service = NSNetService(
            domain = "",
            type = serviceType,
            name = advertised.name,
            port = FileShareConstants.DEFAULT_PORT,
        ).also {
            it.delegate = this
            if (advertised.txtRecord.isNotEmpty()) {
                it.setTXTRecordData(NSNetService.dataFromTXTRecordDictionary(advertised.txtRecord.mapValues { (_, value) -> value.toNSData() }))
            }
            it.publish()
        }
    }

    // delegate 는 강하게 잡히지 않는다. 이 객체가 먼저 사라져도 불리지 않게 끊는다.
    private fun unregister() {
        service?.let {
            it.delegate = null
            it.stop()
        }
        service = null
    }

    @ObjCSignatureOverride
    override fun netService(sender: NSNetService, didNotPublish: Map<Any?, *>) {
        if (sender !== service) return
        appLog.sendDebugLog(DebugLog(BONJOUR_LOG_TAG, "Bonjour 광고 등록 실패($serviceType, $retryCount): $didNotPublish"))
        if (retryCount >= BONJOUR_MAX_RETRIES) {
            appLog.sendErrorLog(ErrorLog(BONJOUR_LOG_TAG, "Bonjour 광고 등록을 $BONJOUR_MAX_RETRIES 번 다시 시도했지만 실패: $serviceType ($didNotPublish)"))
            return
        }
        retryCount++
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(BONJOUR_RETRY_DELAY_MS)
            register()
        }
    }
}

private fun String.toNSData(): NSData = NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)!!

private const val BONJOUR_LOG_TAG = "BonjourAdvertiser"
private const val BONJOUR_MAX_RETRIES = 3
private const val BONJOUR_RETRY_DELAY_MS = 2_000L
