package com.kintmin.desktop.upload

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.Job
import kotlin.test.Test
import kotlin.test.assertFalse

class FileUploaderTest {

    @Test
    fun `닫으면 넘겨받은 엔진도 닫는다`() {
        val engine = MockEngine { respondOk() }

        FileUploader("phone", 1, engine).close()

        assertFalse(engine.coroutineContext[Job]!!.isActive)
    }
}
