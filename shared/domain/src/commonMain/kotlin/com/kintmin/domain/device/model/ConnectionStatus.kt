package com.kintmin.domain.device.model

data class ConnectionStatus(
    val isConnected: Boolean,
    val isWifi: Boolean,
    val isCellular: Boolean,
    val downstreamKbps: Int,
    val upstreamKbps: Int,
)