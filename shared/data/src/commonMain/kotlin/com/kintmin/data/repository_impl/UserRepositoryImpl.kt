package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.PreferencesKey
import com.kintmin.domain.user.repository.UserRepository
import kotlinx.coroutines.flow.firstOrNull

class UserRepositoryImpl constructor(
    private val datastoreUtil: DatastoreUtil,
) : UserRepository {

    override suspend fun setUserId(userId: String): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.UserId, userId)
    }

    override suspend fun getUserId(): Result<String?> {
        return runCatching {
            datastoreUtil.getData(PreferencesKey.UserId).firstOrNull()
        }
    }
}