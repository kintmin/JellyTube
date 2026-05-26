package com.kintmin.desktop.discovery

import com.kintmin.fileshare.FileShareConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import java.net.NetworkInterface
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
    val serviceTypes = listOf(
        FileShareConstants.NSD_SERVICE_TYPE,
        "${FileShareConstants.NSD_SERVICE_TYPE}local.",
    ).distinct()
    println("[NsdDiscovery] discovery started timeoutMs=$timeoutMs serviceTypes=${serviceTypes.joinToString()}")
    val jmdnsList = mutableListOf<JmDNS>()
    try {
        val result = withTimeoutOrNull(timeoutMs) {
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                val listener = object : ServiceListener {
                    override fun serviceAdded(event: ServiceEvent) {
                        println("[NsdDiscovery] serviceAdded name=${event.name} type=${event.type}")
                        // requestServiceInfo로 실제 주소/포트 가져오기
                        event.dns.requestServiceInfo(event.type, event.name, 1_000)
                    }

                    override fun serviceRemoved(event: ServiceEvent) {
                        println("[NsdDiscovery] serviceRemoved name=${event.name} type=${event.type}")
                    }

                    override fun serviceResolved(event: ServiceEvent) {
                        val info = event.info ?: return
                        println("[NsdDiscovery] serviceResolved name=${event.name} addresses=${info.inet4Addresses.joinToString { it.hostAddress }} port=${info.port}")
                        val host = info.inet4Addresses.firstOrNull()?.hostAddress ?: return
                        val port = info.port
                        if (port > 0 && !cont.isCompleted) {
                            cont.resumeWith(Result.success(DiscoveredDevice(hostAddress = host, port = port)))
                        }
                    }
                }

                val localAddresses = NetworkInterface.getNetworkInterfaces().asSequence()
                    .filter { it.isUp && !it.isLoopback && !it.isVirtual && it.supportsMulticast() }
                    .flatMap { networkInterface -> networkInterface.inetAddresses.asSequence() }
                    .filterIsInstance<Inet4Address>()
                    .filterNot { it.isLoopbackAddress || it.isLinkLocalAddress }
                    .toList()
                println("[NsdDiscovery] local IPv4 multicast addresses=${localAddresses.joinToString { it.hostAddress }}")

                val createdJmDns = localAddresses.mapNotNull { address ->
                    runCatching {
                        JmDNS.create(address).also {
                            println("[NsdDiscovery] JmDNS created address=${address.hostAddress}")
                        }
                    }.onFailure { error ->
                        println("[NsdDiscovery] JmDNS create failed address=${address.hostAddress} error=${error.message}")
                    }.getOrNull()
                }.ifEmpty {
                    listOfNotNull(
                        runCatching {
                            JmDNS.create().also { println("[NsdDiscovery] fallback JmDNS created") }
                        }.onFailure { error ->
                            println("[NsdDiscovery] fallback JmDNS create failed error=${error.message}")
                        }.getOrNull(),
                    )
                }
                println("[NsdDiscovery] JmDNS listener count=${createdJmDns.size}")

                jmdnsList += createdJmDns
                createdJmDns.forEach { jmdns ->
                    serviceTypes.forEach { serviceType ->
                        jmdns.addServiceListener(serviceType, listener)
                        println("[NsdDiscovery] listener added type=$serviceType")
                    }
                }

                cont.invokeOnCancellation {
                    createdJmDns.forEach { jmdns ->
                        serviceTypes.forEach { serviceType ->
                            runCatching { jmdns.removeServiceListener(serviceType, listener) }
                        }
                    }
                }
            }
        }
        if (result == null) {
            println("[NsdDiscovery] discovery timeout")
        }
        result
    } finally {
        jmdnsList.forEach { jmdns -> runCatching { jmdns.close() } }
        println("[NsdDiscovery] discovery closed")
    }
}
