package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL

private const val SHARE_DIR_NAME = "audio-share"
private const val FALLBACK_EXT = "m4a"

/**
 * 시트가 파일 이름을 그대로 보여 주는데 디스크 파일명은 UUID 라서, 표시 이름의 임시 복사본을 넘기고
 * 시트가 닫히면 지운다. 앞선 시도가 남긴 복사본은 매번 먼저 지운다.
 */
@OptIn(ExperimentalForeignApi::class)
private fun shareAudioFile(fileFullPath: String, title: String, artist: String): Boolean {
    val fileManager = NSFileManager.defaultManager
    val shareDir = NSTemporaryDirectory() + SHARE_DIR_NAME
    fileManager.removeItemAtPath(shareDir, error = null)
    if (!fileManager.createDirectoryAtPath(shareDir, withIntermediateDirectories = true, attributes = null, error = null)) {
        return false
    }

    val ext = fileFullPath.substringAfterLast('/').substringAfterLast('.', missingDelimiterValue = FALLBACK_EXT)
    val targetPath = "$shareDir/${shareFileName(title, artist, ext)}"
    val cleanUp = { fileManager.removeItemAtPath(shareDir, error = null); Unit }

    if (!fileManager.copyItemAtPath(fileFullPath, toPath = targetPath, error = null)) {
        cleanUp()
        return false
    }
    val isStarted = presentShareSheet(listOf(NSURL.fileURLWithPath(targetPath)), onFinished = cleanUp)
    if (!isStarted) cleanUp()
    return isStarted
}

@Composable
actual fun rememberAudioFileSharer(): AudioFileSharer = remember { AudioFileSharer { path, title, artist -> shareAudioFile(path, title, artist) } }
