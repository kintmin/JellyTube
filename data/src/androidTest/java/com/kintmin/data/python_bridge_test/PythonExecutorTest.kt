package com.kintmin.data.python_bridge_test

import android.content.Context
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.data.local_file.FileManagerImpl
import com.kintmin.data.python_bridge.PythonExecutor
import com.kintmin.data.python_bridge.PythonExecutorImpl
import com.kintmin.data.python_bridge.model.YoutubeDownloadDto
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class PythonExecutorTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var pythonExecutor: PythonExecutor


    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        pythonExecutor = PythonExecutorImpl(context)
    }


    @Test
    fun downloadYoutubeMedia_smokeTest() = runTest {
        val dto = pythonExecutor.downloadYoutubeMedia("", "").getOrThrow()

        assert("알 수 없음" == dto.title)
        assert("" == dto.thumbnailDownloadUrl)
        assert("0" == dto.duration)
        assert("알 수 없음" == dto.uploader)
        assert("" == dto.description)
    }
}