package com.kintmin.domain.device.repository

import com.kintmin.domain.device.model.ConnectionStatus
import com.kintmin.domain.device.model.DeviceMemory

interface DeviceStatusRepository {

    fun getSystemMemory(): Result<DeviceMemory>
    fun getConnectionStatus(): Result<ConnectionStatus>
}