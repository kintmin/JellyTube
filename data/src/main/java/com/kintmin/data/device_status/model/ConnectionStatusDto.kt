package com.kintmin.data.device_status.model

data class ConnectionStatusDto(
    val isConnected: Boolean,
    val isWifi: Boolean,
    val isCellular: Boolean,
    val downstreamKbps: Int,
    val upstreamKbps: Int,
)