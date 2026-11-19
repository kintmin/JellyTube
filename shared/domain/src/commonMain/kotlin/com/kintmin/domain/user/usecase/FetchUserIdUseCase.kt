package com.kintmin.domain.user.usecase

import com.kintmin.domain.user.repository.UserRepository

class FetchUserIdUseCase constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(): Result<String?> {
        return userRepository.getUserId()
    }
}
