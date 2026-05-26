package com.kintmin.desktop.discovery

import com.kintmin.fileshare.FileShareConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

data class DiscoveredDevice(
    val hostAddress: String,
    val port: Int,
)

/**
 * mDNS/DNS-SD로 같은 네트워크의 Android 기기를 탐색한다.
 * @param timeoutMs discovery 최대 대기 시간 (ms)
 */
suspend fun discoverDevice(timeoutMs: Long = 3_000L): DiscoveredDevice? = withContext(Dispatchers.IO) {
    var jmdns: JmDNS? = null
    try {
        val result = withTimeoutOrNull(timeoutMs) {
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                jmdns = JmDNS.create()
                val listener = object : ServiceListener {
                    override fun serviceAdded(event: ServiceEvent) {
                        // requestServiceInfo로 실제 주소/포트 가져오기
                        event.dns.requestServiceInfo(event.type, event.name, 1_000)
                    }

                    override fun serviceRemoved(event: ServiceEvent) {}

                    override fun serviceResolved(event: ServiceEvent) {
                        val info = event.info ?: return
                        val addresses = info.inet4Addresses
                        val host = addresses?.firstOrNull()?.hostAddress ?: return
                        val port = info.port
                        if (port > 0 && !cont.isCompleted) {
                            cont.resumeWith(Result.success(DiscoveredDevice(hostAddress = host, port = port)))
                        }
                    }
                }

                jmdns?.addServiceListener(FileShareConstants.NSD_SERVICE_TYPE, listener)

                cont.invokeOnCancellation {
                    runCatching { jmdns?.removeServiceListener(FileShareConstants.NSD_SERVICE_TYPE, listener) }
                }
            }
        }
        result
    } finally {
        runCatching { jmdns?.close() }
    }
}
