package com.kintmin.domain.user.usecase

import com.kintmin.domain.user.repository.UserRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.FirebaseEvent
import java.util.UUID

class RegisterUserUseCase constructor(
    private val userRepository: UserRepository,
    private val appLog: AppLog,
) {

    suspend operator fun invoke() = runCatching {
        val savedUserId = userRepository.getUserId().getOrThrow()
        if (savedUserId != null) return@runCatching savedUserId

        val newUserId = UUID.randomUUID().toString()
        userRepository.setUserId(newUserId).onSuccess {
            appLog.sendFirebaseEvent(FirebaseEvent.SuccessRegisterUser(newUserId))
        }
        newUserId
    }.onFailure { error ->
        appLog.sendFirebaseEvent(FirebaseEvent.FailedRegisterUser(error))
    }.onSuccess { userId ->
        appLog.setLogConfig(userId)
    }
}
