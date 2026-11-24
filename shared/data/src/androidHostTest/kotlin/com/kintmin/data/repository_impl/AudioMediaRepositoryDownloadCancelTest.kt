package com.kintmin.data.repository_impl

import com.kintmin.data.local_file.FileManager
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.python_bridge.model.YoutubeDownloadDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test

class AudioMediaRepositoryDownloadCancelTest {

    @Test
    fun `받는 도중 취소되면 끊기지 않고 만들어진 그 곡의 파일만 지운다`() = runTest {
        var fileName = ""
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val fileManager = mockk<FileManager>(relaxed = true) {
            every { getAudioDownloadBasePath(any()) } answers {
                fileName = firstArg()
                Result.success("/music/$fileName")
            }
            coEvery { listAudioAndImageFileFullPaths() } answers { Result.success(listOf("/music/$fileName.m4a", "/music/other.m4a")) }
            every { getFileNameWithExt(any()) } answers { Result.success(firstArg<String>().substringAfterLast('/')) }
            coEvery { deleteFileAtFullPath(any()) } returns Result.success(Unit)
        }
        val pythonExecutor = mockk<PythonExecutor> {
            coEvery { downloadYoutubeMedia(any(), any(), any()) } coAnswers {
                started.complete(Unit)
                withContext(NonCancellable) { finish.await() }
                Result.success(YoutubeDownloadDto("t", "", "1", "u", "", "$fileName.m4a"))
            }
        }
        val repository = AudioMediaRepositoryImpl(mockk(), mockk(), mockk(), fileManager, pythonExecutor)

        val job = launch { repository.downloadYoutubeAudio("https://www.youtube.com/watch?v=a", isSecret = false) }
        started.await()
        val cancelling = launch { job.cancelAndJoin() }
        finish.complete(Unit)
        cancelling.join()

        coVerify(exactly = 1) { fileManager.deleteFileAtFullPath("/music/$fileName.m4a") }
        coVerify(exactly = 0) { fileManager.deleteFileAtFullPath("/music/other.m4a") }
    }
}
