package com.kintmin.jellytube.data_migration

import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.model.DebugLog
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.presentation.port.MigrationSourceDiscovery
import com.kintmin.presentation.port.model.MigrationSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import java.util.Collections
import kotlin.coroutines.resume

internal class NsdMigrationSourceDiscovery(
    context: Context,
    private val appLog: AppLog,
) : MigrationSourceDiscovery {

    private val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    // TXT 는 주소를 풀어야 읽히는데 NSD 는 한 번에 하나만 푼다. 버전이 다르면 manifest 단계가 막는다.
    override suspend fun discover(): List<MigrationSource> {
        val names = Collections.synchronizedSet(linkedSetOf<String>())
        val changes = Channel<Unit>(Channel.CONFLATED)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(info: NsdServiceInfo) {
                if (names.add(info.serviceName)) changes.trySend(Unit)
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                if (names.remove(info.serviceName)) changes.trySend(Unit)
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                appLog.sendErrorLog(ErrorLog(LOG_TAG, "기기 탐색 시작 실패: errorCode=$errorCode"))
                changes.close()
            }

            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }
        runCatching {
            manager.discoverServices(FileShareConstants.MIGRATION_NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            appLog.sendErrorLog(ErrorLog(LOG_TAG, "기기 탐색 시작 실패", it))
            return emptyList()
        }
        try {
            withTimeoutOrNull(DISCOVERY_TIMEOUT_MILLIS) {
                if (changes.receiveCatching().isClosed) return@withTimeoutOrNull
                while (true) {
                    val next = withTimeoutOrNull(SETTLE_MILLIS) { changes.receiveCatching() } ?: break
                    if (next.isClosed) break
                }
            }
        } finally {
            runCatching { manager.stopServiceDiscovery(listener) }
        }
        return names.toList().toMigrationSources()
    }

    @Suppress("DEPRECATION")
    override suspend fun resolve(source: MigrationSource): MigrationEndpoint? = withTimeoutOrNull(RESOLVE_TIMEOUT_MILLIS) {
        suspendCancellableCoroutine { continuation ->
            val target = NsdServiceInfo().apply {
                serviceName = source.name
                serviceType = FileShareConstants.MIGRATION_NSD_SERVICE_TYPE
            }
            manager.resolveService(
                target,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                        appLog.sendDebugLog(DebugLog(LOG_TAG, "기기 주소 풀기 실패: ${source.name} errorCode=$errorCode"))
                        if (continuation.isActive) continuation.resume(null)
                    }

                    override fun onServiceResolved(info: NsdServiceInfo) {
                        if (continuation.isActive) continuation.resume(info.toEndpoint())
                    }
                },
            )
        }
    }
}

internal fun List<String>.toMigrationSources(): List<MigrationSource> =
    distinct().sorted().map { MigrationSource(name = it, isCompatible = true) }

// Ktor 에 넘길 주소다. IPv6 는 범위 접미사(%wlan0)와 괄호 처리가 따라와 IPv4 로 고정한다.
@Suppress("DEPRECATION")
private fun NsdServiceInfo.toEndpoint(): MigrationEndpoint? {
    val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        hostAddresses.filterIsInstance<Inet4Address>().firstOrNull()
    } else {
        host as? Inet4Address
    }
    return address?.hostAddress?.let { MigrationEndpoint(host = it, port = port) }
}

// 마지막 변화 뒤 이만큼 조용하면 끝낸다. 짧으면 늦게 보인 두 번째 기기를 놓치고 첫 기기에 자동으로 붙는다.
private const val SETTLE_MILLIS = 3_000L
private const val DISCOVERY_TIMEOUT_MILLIS = 10_000L
private const val RESOLVE_TIMEOUT_MILLIS = 10_000L

private const val LOG_TAG = "NsdMigrationSourceDiscovery"
