package com.kintmin.domain.user

import com.kintmin.domain.device.model.AppEnvironment
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.domain.user.repository.UserRepository
import com.kintmin.domain.user.usecase.RegisterUserUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RegisterUserUseCaseTest {

    private val userRepository: UserRepository = mockk {
        coEvery { getUserId() } returns Result.success("user")
    }
    private val deviceStatusRepository: DeviceStatusRepository = mockk()
    private val appLog: AppLog = mockk(relaxed = true)
    private val useCase = RegisterUserUseCase(userRepository, deviceStatusRepository, appLog)

    @Test
    fun `앱 버전·OS·기기를 로그에 남긴다`() = runTest {
        every { deviceStatusRepository.getAppEnvironment() } returns Result.success(
            AppEnvironment(appVersion = "1.2.2(26)", osVersion = "Android 14 (API 34)", deviceModel = "Google Pixel 7"),
        )

        useCase()

        verify {
            appLog.sendDebugLog(
                match<DebugLog> {
                    it.message == "version=1.2.2(26) os=Android 14 (API 34) device=Google Pixel 7"
                },
            )
        }
    }

    @Test
    fun `기기 정보를 못 읽어도 사용자 등록은 이어진다`() = runTest {
        every { deviceStatusRepository.getAppEnvironment() } returns Result.failure(IllegalStateException())

        val result = useCase()

        assertEquals("user", result.getOrNull())
        verify { appLog.setLogConfig("user") }
    }
}
