package com.kintmin.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIPopoverPresentationController
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

/**
 * 로그 파일을 공유 시트로 넘긴다. Android 와 달리 수신자를 지정할 공개 수단이 없어
 * 어디로 보낼지는 사용자가 고르고, 시트가 항상 떠서 `NoShareTarget` 이 서지 않는다.
 */
@OptIn(ExperimentalForeignApi::class)
private fun shareAppLogs(fileFullPaths: List<String>): AppLogShareResult {
    if (fileFullPaths.isEmpty()) return AppLogShareResult.NoLogFiles
    val fileUrls = fileFullPaths.map { NSURL.fileURLWithPath(it) }

    val presenter = topViewController() ?: return AppLogShareResult.Failed
    val controller = UIActivityViewController(
        activityItems = fileUrls,
        applicationActivities = null,
    )

    // iPad 는 UIKit 이 이 화면을 popover 로 준다. 기준 위치가 없으면 present 하는 순간 터진다.
    // `modalPresentationStyle` 을 손대면 안 된다 — popover 가 아닌 기기에서 아래 블록이 건너뛰어지고
    // 기준 위치 없는 popover 만 남는다.
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

    presenter.presentViewController(controller, animated = true, completion = null)
    return AppLogShareResult.Started
}

@Composable
actual fun rememberAppLogSharer(): AppLogSharer = remember { AppLogSharer { shareAppLogs(it) } }
