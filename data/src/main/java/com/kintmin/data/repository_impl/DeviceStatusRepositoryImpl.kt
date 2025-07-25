package com.kintmin.data.repository_impl

import com.kintmin.data.device_status.DeviceStatus
import com.kintmin.domain.device.model.ConnectionStatus
import com.kintmin.domain.device.model.DeviceMemory
import com.kintmin.domain.device.repository.DeviceStatusRepository
import javax.inject.Inject

class DeviceStatusRepositoryImpl @Inject constructor(
    private val deviceStatus: DeviceStatus,
) : DeviceStatusRepository {

    override fun getSystemMemory(): Result<DeviceMemory> {
        TODO("Not yet implemented")
    }

    override fun getConnectionStatus(): Result<ConnectionStatus> {
        TODO("Not yet implemented")
    }
}