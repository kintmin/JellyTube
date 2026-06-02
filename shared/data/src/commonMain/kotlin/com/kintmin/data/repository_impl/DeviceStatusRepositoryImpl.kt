package com.kintmin.data.repository_impl

import com.kintmin.data.device_status.DeviceStatus
import com.kintmin.domain.device.model.ConnectionStatus
import com.kintmin.domain.device.model.DeviceMemory
import com.kintmin.domain.device.repository.DeviceStatusRepository

class DeviceStatusRepositoryImpl constructor(
    private val deviceStatus: DeviceStatus,
) : DeviceStatusRepository {

    override fun getSystemMemory(): Result<DeviceMemory> {
        return deviceStatus.getSystemMemory().map {
            DeviceMemory(
                availableRemMemory = it.availableRemMemory,
                isLowRemMemory = it.isLowRemMemory,
                availableStorage = it.availableStorage,
            )
        }
    }

    override fun getConnectionStatus(): Result<ConnectionStatus> {
        return deviceStatus.getConnectionStatus().map {
            ConnectionStatus(
                isConnected = it.isConnected,
                isWifi = it.isWifi,
                isCellular = it.isCellular,
                downstreamKbps = it.downstreamKbps,
                upstreamKbps = it.upstreamKbps,
            )
        }
    }
}