package com.kintmin.data.device_status.model

data class DeviceMemoryDto(
    val availableRemMemory: Long,
    val isLowRemMemory: Boolean,
    val availableStorage: Long,
)