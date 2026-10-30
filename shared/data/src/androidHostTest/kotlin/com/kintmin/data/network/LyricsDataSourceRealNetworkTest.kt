package com.kintmin.data.network

import com.kintmin.data.network.dataSourceImpl.LyricsDataSourceImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsDataSourceRealNetworkTest {

    @Test
    fun search_returnsSuccess_forVariousQueries() = runTest {
        val client = createHttpClient()
        val dataSource = LyricsDataSourceImpl(client)

        val queries = listOf(
            "하늘끝에서",
        )

        for (query in queries) {
            val result = dataSource.search(query)
            assertTrue("검색 실패: $query -> ${result.exceptionOrNull()}", result.isSuccess)
            assertTrue("결과 없음: $query", result.getOrThrow().isNotEmpty())
        }
    }
}
