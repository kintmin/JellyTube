package com.kintmin.domain.user.usecase

import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.domain.user.repository.UserRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.SentryEvent
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RegisterUserUseCase constructor(
    private val userRepository: UserRepository,
    private val deviceStatusRepository: DeviceStatusRepository,
    private val appLog: AppLog,
) {

    suspend operator fun invoke() = suspendRunCatching {
        deviceStatusRepository.getAppEnvironment().onSuccess {
            appLog.sendDebugLog(
                DebugLog("AppStart", "version=${it.appVersion} os=${it.osVersion} device=${it.deviceModel}"),
            )
        }
        val savedUserId = userRepository.getUserId().getOrThrow()
        if (savedUserId != null) return@suspendRunCatching savedUserId

        val newUserId = Uuid.random().toString()
        userRepository.setUserId(newUserId).onSuccess {
            appLog.sendSentryEvent(SentryEvent.SuccessRegisterUser(newUserId))
        }
        newUserId
    }.onFailure { error ->
        appLog.sendSentryEvent(SentryEvent.FailedRegisterUser(error))
    }.onSuccess { userId ->
        appLog.setLogConfig(userId)
    }
}
