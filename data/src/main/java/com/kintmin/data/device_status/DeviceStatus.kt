package com.kintmin.data.device_status

import com.kintmin.data.device_status.model.ConnectionStatusDto
import com.kintmin.data.device_status.model.DeviceMemoryDto

interface DeviceStatus {

    fun getSystemMemory(): Result<DeviceMemoryDto>
    fun getConnectionStatus(): Result<ConnectionStatusDto>
}