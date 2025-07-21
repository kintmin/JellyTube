package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.preference_key.StringPreferenceKey
import com.kintmin.domain.user.repository.UserRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val datastoreUtil: DatastoreUtil,
) : UserRepository {

    override suspend fun registerUser(userId: String): Result<Unit> {
        return datastoreUtil.updateStringData(StringPreferenceKey.UserId, userId)
    }

    override suspend fun getUserId(): Result<String?> {
        return runCatching {
            datastoreUtil.userId.firstOrNull()
        }
    }
}