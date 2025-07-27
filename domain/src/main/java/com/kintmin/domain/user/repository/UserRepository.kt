package com.kintmin.domain.user.repository

interface UserRepository {
    suspend fun setUserId(userId: String): Result<Unit>
    suspend fun getUserId(): Result<String?>
}