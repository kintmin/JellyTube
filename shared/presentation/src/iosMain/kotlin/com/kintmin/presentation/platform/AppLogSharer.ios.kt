package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIPopoverPresentationController
import platform.UIKit.UIModalPresentationPopover
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

/** `shared:data` 의 `FileManagerImpl` 이 쓰는 이름과 같아야 한다. 다르면 빈 목록이 된다. */
private const val LOG_DIR_NAME = "app_logs"
private const val LOG_FILE_EXTENSION = "log"

/**
 * 로그 파일을 iOS 공유 시트로 넘긴다.
 *
 * Android 는 메일 앱을 직접 겨냥하지만(`ACTION_SEND_MULTIPLE` + 수신자),
 * **iOS 에는 "이 주소로 보내는 메일 앱"을 고르게 하는 공개 수단이 없다.**
 * `UIActivityViewController` 로 파일만 넘기고 어디로 보낼지는 사용자가 고른다.
 * `NoShareTarget` 이 서지 않는 이유도 이것이다 — iOS 공유 시트는 항상 뜬다.
 */
@OptIn(ExperimentalForeignApi::class)
private fun shareAppLogs(): AppLogShareResult {
    val logDirectory = documentDirectory()?.appendPathComponent(LOG_DIR_NAME)
        ?: return AppLogShareResult.Failed

    val fileUrls = (
        NSFileManager.defaultManager.contentsOfDirectoryAtPath(logDirectory, error = null)
            ?.filterIsInstance<String>()
            ?: emptyList()
        )
        .filter { name -> name.endsWith(".$LOG_FILE_EXTENSION") }
        .sortedDescending()
        .map { name -> NSURL.fileURLWithPath(logDirectory.appendPathComponent(name)) }

    if (fileUrls.isEmpty()) return AppLogShareResult.NoLogFiles

    val presenter = topViewController() ?: return AppLogShareResult.Failed
    val controller = UIActivityViewController(
        activityItems = fileUrls,
        applicationActivities = null,
    )

    // iPad 는 popover 라 기준 위치가 없으면 present 하는 순간 터진다. 화면 가운데를 준다.
    controller.popoverPresentationController?.let { popover ->
        popover.sourceView = presenter.view
        presenter.view.bounds.useContents {
            popover.sourceRect = CGRectMake(
                x = size.width / 2.0,
                y = size.height / 2.0,
                width = 0.0,
                height = 0.0,
            )
        }
    }
    controller.modalPresentationStyle = UIModalPresentationPopover

    presenter.presentViewController(controller, animated = true, completion = null)
    return AppLogShareResult.Started
}

@Composable
actual fun rememberAppLogSharer(): AppLogSharer = remember { AppLogSharer { shareAppLogs() } }

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String? = NSFileManager.defaultManager.URLForDirectory(
    directory = NSDocumentDirectory,
    inDomain = NSUserDomainMask,
    appropriateForURL = null,
    create = false,
    error = null,
)?.path

private fun String.appendPathComponent(component: String): String = trimEnd('/') + "/" + component
