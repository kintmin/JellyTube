package com.kintmin.domain.secret_mode.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SecretModeRepository {
    // 메모리에만 둔다 — 앱이 다시 뜨면 꺼진 상태다.
    val isEnabledFlow: StateFlow<Boolean>
    fun setEnabled(value: Boolean)

    fun getMinutesFlow(): Flow<Int>
    suspend fun updateMinutes(minutes: Int): Result<Unit>

    companion object {
        const val DEFAULT_MINUTES = 30
    }
}
