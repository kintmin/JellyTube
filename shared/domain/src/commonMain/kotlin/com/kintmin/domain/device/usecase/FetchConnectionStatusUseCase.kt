package com.kintmin.domain.device.usecase

import com.kintmin.domain.device.model.ConnectionStatus
import com.kintmin.domain.device.repository.DeviceStatusRepository

class FetchConnectionStatusUseCase(
    private val deviceStatusRepository: DeviceStatusRepository,
) {
    operator fun invoke(): Result<ConnectionStatus> = deviceStatusRepository.getConnectionStatus()
}
