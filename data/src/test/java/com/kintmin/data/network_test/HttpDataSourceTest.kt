package com.kintmin.data.network_test

import com.kintmin.data.network.dataSource.HttpDataSource
import com.kintmin.data.network.dataSourceImpl.HttpDataSourceImpl
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HttpDataSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var dataSource: HttpDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val client = OkHttpClient.Builder().build()
        dataSource = HttpDataSourceImpl(client)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `downloadImage - 200이면 body bytes를 반환한다`() = runTest {
        val expected = byteArrayOf(1, 2, 3, 4, 5)

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(okio.Buffer().write(expected))
        )

        val url = server.url("/image.png").toString()
        val result = dataSource.downloadImage(url)

        assertTrue(result.isSuccess)
        assertArrayEquals(expected, result.getOrThrow())

        val recorded = server.takeRequest()
        assertEquals("/image.png", recorded.path)
        assertEquals("GET", recorded.method)
    }

    @Test
    fun `downloadImage - 404이면 failure를 반환한다`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("not found")
        )

        val url = server.url("/missing.png").toString()
        val result = dataSource.downloadImage(url)

        assertTrue(result.isFailure)
    }
}