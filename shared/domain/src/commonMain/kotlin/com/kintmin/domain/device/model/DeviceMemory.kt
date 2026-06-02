package com.kintmin.domain.device.model

data class DeviceMemory(
    val availableRemMemory: Long,
    val isLowRemMemory: Boolean,
    val availableStorage: Long,
)