package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.PreferencesKey
import com.kintmin.domain.secret_mode.repository.SecretModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class SecretModeRepositoryImpl constructor(
    private val datastoreUtil: DatastoreUtil,
) : SecretModeRepository {

    private val isEnabled = MutableStateFlow(false)
    override val isEnabledFlow: StateFlow<Boolean> = isEnabled.asStateFlow()

    override fun setEnabled(value: Boolean) {
        isEnabled.value = value
    }

    override fun getMinutesFlow(): Flow<Int> {
        return datastoreUtil.getData(PreferencesKey.SecretModeMinutes).map {
            it ?: SecretModeRepository.DEFAULT_MINUTES
        }
    }

    override suspend fun updateMinutes(minutes: Int): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.SecretModeMinutes, minutes)
    }
}
