package com.kintmin.data.device_status

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.os.StatFs
import com.kintmin.data.device_status.model.ConnectionStatusDto
import com.kintmin.data.device_status.model.DeviceMemoryDto

class DeviceStatusImpl constructor(
    private val context: Context,
): DeviceStatus {

    override fun getSystemMemory(): Result<DeviceMemoryDto> {
        return runCatching {
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
            val stat = StatFs(Environment.getDataDirectory().path)

            DeviceMemoryDto(
                availableRemMemory = memoryInfo.availMem,
                isLowRemMemory = memoryInfo.lowMemory,
                availableStorage = stat.availableBytes,
            )
        }
    }

    override fun getConnectionStatus(): Result<ConnectionStatusDto> {
        return runCatching {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val downstreamKbps = capabilities?.linkDownstreamBandwidthKbps ?: 0
            val upstreamKbps = capabilities?.linkUpstreamBandwidthKbps ?: 0

            ConnectionStatusDto(
                isConnected = isConnected,
                isWifi = isWifi,
                isCellular = isCellular,
                downstreamKbps = downstreamKbps,
                upstreamKbps = upstreamKbps,
            )
        }
    }
}