package com.kintmin.data.device_status

import com.kintmin.data.device_status.model.ConnectionStatusDto
import com.kintmin.data.device_status.model.DeviceMemoryDto
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSProcessInfo
import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_uses_interface_type
import platform.darwin.dispatch_queue_create
import kotlin.concurrent.Volatile

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class DeviceStatusImpl : DeviceStatus {

    private val monitor = nw_path_monitor_create()
    private val queue = dispatch_queue_create("com.kintmin.data.device_status.NWPathMonitor", null)

    @Volatile
    private var cachedConnection: ConnectionStatusDto = ConnectionStatusDto(
        isConnected = false,
        isWifi = false,
        isCellular = false,
        downstreamKbps = 0,
        upstreamKbps = 0,
    )

    init {
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_set_update_handler(monitor) { path ->
            path ?: return@nw_path_monitor_set_update_handler
            cachedConnection = ConnectionStatusDto(
                isConnected = nw_path_get_status(path) == nw_path_status_satisfied,
                isWifi = nw_path_uses_interface_type(path, nw_interface_type_wifi),
                isCellular = nw_path_uses_interface_type(path, nw_interface_type_cellular),
                downstreamKbps = 0,
                upstreamKbps = 0,
            )
        }
        nw_path_monitor_start(monitor)
    }

    // iOS는 앱에게 가용 RAM을 공식 API로 제공하지 않아, 관측용으로 총 물리 메모리를 기록한다.
    override fun getSystemMemory(): Result<DeviceMemoryDto> = runCatching {
        val totalMemory = NSProcessInfo.processInfo.physicalMemory.toLong()
        val attributes = NSFileManager.defaultManager.attributesOfFileSystemForPath(
            NSHomeDirectory(),
            null,
        )
        val freeStorage = (attributes?.get(NSFileSystemFreeSize) as? NSNumber)?.longLongValue ?: 0L
        DeviceMemoryDto(
            availableRemMemory = totalMemory,
            isLowRemMemory = false,
            availableStorage = freeStorage,
        )
    }

    override fun getConnectionStatus(): Result<ConnectionStatusDto> = runCatching {
        cachedConnection
    }
}
