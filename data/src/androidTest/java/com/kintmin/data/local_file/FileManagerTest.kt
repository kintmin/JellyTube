package com.kintmin.data.local_file

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.data.local_file.model.Ext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class FileManagerTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var fileManager: FileManager

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        fileManager = FileManagerImpl(context)
    }

    @After
    fun tearDown() {
        runCatching {
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.deleteRecursively()
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.deleteRecursively()
            context.cacheDir.deleteRecursively()
        }
    }

    @Test
    fun getFileNameWithExt_fromFullPath_returnsName(): Unit = runTest {
        val fileFullPath = "/a/b/c/sample.jpeg"
        val expectedPath = "sample.jpeg"

        val result = fileManager.getFileNameWithExt(fileFullPath)

        assert(result.isSuccess)
        assert(expectedPath == result.getOrThrow())
    }

    @Test
    fun getFileNameWithExt_whenOnlyName_returnsSame() {
        val fileFullPath = "hello.webp"
        val expectedPath = "hello.webp"

        val result = fileManager.getFileNameWithExt(fileFullPath)

        assert(result.isSuccess)
        assert(expectedPath == result.getOrThrow())
    }

    @Test
    fun getFullPathWithExt_withValidExt_pointsToExpectedExternalDir() {
        val fileNameWithExt = "img1.jpeg"
        val expectedBaseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val expected = File(expectedBaseDir, fileNameWithExt).absolutePath

        val result = fileManager.getFullPathWithExt(fileNameWithExt)

        assert(result.isSuccess)
        assert(expected == result.getOrThrow())
    }

    @Test
    fun getFullPathWithExt_withoutDot_returnsFailure() {
        val result = fileManager.getFullPathWithExt("no_extension")
        assert(result.isFailure)
    }

    @Test
    fun getFullPathWithExt_withUnknownExt_returnsFailure() {
        val result = fileManager.getFullPathWithExt("img1.zzz")
        assert(result.isFailure)
    }

    @Test
    fun getFullPathWithExt_isCaseInsensitiveForExt() {
        val fileNameWithExt = "img1.JpEg"
        val expectedBaseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val expected = File(expectedBaseDir, fileNameWithExt).absolutePath

        val result = fileManager.getFullPathWithExt(fileNameWithExt)

        assert(result.isSuccess)
        assert(expected == result.getOrThrow())
    }

    @Test
    fun getFullPathWithExt_withFileNameAndExt_buildsCorrectPath() {
        val fileName = "photo_123"
        val ext = Ext.JPEG
        val expectedBaseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val expected = File(expectedBaseDir, "$fileName.$ext").absolutePath

        val result = fileManager.getFullPathWithExt(fileName, ext)

        assert(expected == result.getOrThrow())
    }

    @Test
    fun saveImageWithCompression_savesFile_andReturnsTargetExt() = runTest {
        val fileName = "compressed_test"
        val pngBytes = createPngByteArray(8, 8)

        val result = fileManager.saveImageWithCompression(pngBytes, fileName)
        assert(result.isSuccess)

        val returnedExt = result.getOrThrow()
        val expectedExt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Ext.WEBP else Ext.JPEG
        assert(expectedExt == returnedExt)

        val fullPathResult = fileManager.getFullPathWithExt(fileName, returnedExt)
        val savedPath = fullPathResult.getOrThrow()

        val savedFile = File(savedPath)
        assertTrue("저장된 파일이 존재해야 합니다. path=$savedPath", savedFile.exists())
        assertTrue("저장된 파일 크기는 0보다 커야 합니다.", savedFile.length() > 0L)
    }

    @Test
    fun saveImageWithCompression_withInvalidBytes_returnsFailure() = runTest {
        val fileName = "invalid_image"
        val invalid = byteArrayOf(1, 2, 3, 4, 5)

        val result = fileManager.saveImageWithCompression(invalid, fileName)
        assertTrue(result.isFailure)
    }

    @Test
    fun deleteFile_whenFileExists_deletesIt() = runTest {
        val fileNameWithExt = "to_delete.jpeg"
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val target = File(baseDir, fileNameWithExt)
        target.parentFile?.mkdirs()
        target.writeBytes(byteArrayOf(1, 2, 3))

        assertTrue(target.exists())

        val result = fileManager.deleteFile(fileNameWithExt)
        assertTrue(result.isSuccess)
        assertFalse("파일이 삭제되어야 합니다.", target.exists())
    }

    @Test
    fun deleteFile_whenNoSuchFile_returnsSuccess_andStillNotExists() = runTest {
        val fileNameWithExt = "missing.jpeg"

        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val target = File(baseDir, fileNameWithExt)
        if (target.exists()) target.delete()

        val result = fileManager.deleteFile(fileNameWithExt)
        assertTrue(result.isSuccess)
        assertFalse(target.exists())
    }

    @Test
    fun deleteFile_withInvalidFileNameWithoutExt_returnsFailure() = runTest {
        val result = fileManager.deleteFile("no_extension")
        assertTrue(result.isFailure)
    }

    @Test
    fun clearDiskCache_deletesCacheDirContents() {
        val cacheFile = File(context.cacheDir, "temp.txt")
        cacheFile.writeText("hello")
        assertTrue(cacheFile.exists())

        val result = fileManager.clearDiskCache()
        assertTrue(result.isSuccess)

        assertFalse("캐시 파일이 삭제되어야 합니다.", cacheFile.exists())
    }

    private fun createPngByteArray(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
        bitmap.recycle()
        return bos.toByteArray()
    }
}