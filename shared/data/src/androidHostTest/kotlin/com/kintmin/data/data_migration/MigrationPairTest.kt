package com.kintmin.data.data_migration

import com.kintmin.data.network.dataSourceImpl.MigrationDataSourceImpl
import com.kintmin.data.repository_impl.asPairFailure
import com.kintmin.domain.data_migration.model.MigrationPairRejectedException
import com.kintmin.domain.data_migration.model.MigrationSourceClosedException
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.MigrationPairRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationPairTest {

    private var sentRequest: MigrationPairRequest? = null

    private fun dataSource(status: HttpStatusCode, body: String = "") = MigrationDataSourceImpl(
        client = HttpClient(
            MockEngine { request ->
                assertEquals(FileShareConstants.MIGRATION_PAIR_PATH, request.url.encodedPath)
                sentRequest = Json.decodeFromString((request.body as TextContent).text)
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ) {
            install(ContentNegotiation) { json() }
        },
        localDeviceName = { "iPhone" },
    )

    private suspend fun pairFailure(status: HttpStatusCode): Throwable? =
        dataSource(status).pair("host", 1, "123456").exceptionOrNull()?.asPairFailure()

    @Test
    fun `코드와 이 기기 이름을 보내고 토큰을 받는다`() = runTest {
        val token = dataSource(HttpStatusCode.OK, """{"token":"t"}""").pair("host", 1, "123456").getOrThrow()

        assertEquals("t", token)
        assertEquals(MigrationPairRequest(code = "123456", deviceName = "iPhone"), sentRequest)
    }

    @Test
    fun `401 은 코드 틀림 404 는 보내기 세션 없음으로 나눈다`() = runTest {
        assertTrue(pairFailure(HttpStatusCode.Unauthorized) is MigrationPairRejectedException)
        assertTrue(pairFailure(HttpStatusCode.NotFound) is MigrationSourceClosedException)
    }

    @Test
    fun `그 밖의 실패는 그대로 둔다`() = runTest {
        assertTrue(pairFailure(HttpStatusCode.InternalServerError) is ServerResponseException)
    }
}
