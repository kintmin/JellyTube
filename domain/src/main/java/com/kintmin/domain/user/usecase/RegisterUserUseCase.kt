package com.kintmin.domain.user.usecase

import com.kintmin.domain.user.repository.UserRepository
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import java.util.UUID
import javax.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val log: Log,
) {

    suspend operator fun invoke() = runCatching {
        val savedUserId = userRepository.getUserId().getOrThrow()
        if (savedUserId != null) return@runCatching savedUserId

        val newUserId = UUID.randomUUID().toString()
        userRepository.registerUser(newUserId).onSuccess {
            log.sendFirebaseEvent(FirebaseEvent.SuccessRegisterUser(newUserId))
        }
        newUserId
    }.onFailure { error ->
        log.sendFirebaseEvent(FirebaseEvent.FailedRegisterUser(error))
    }
}