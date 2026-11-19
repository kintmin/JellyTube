package com.kintmin.presentation.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

/** 시트를 띄우지 못하면 false. [onFinished] 는 시트가 닫힌 뒤(선택 여부와 무관하게) 불린다. */
@OptIn(ExperimentalForeignApi::class)
internal fun presentShareSheet(items: List<NSURL>, onFinished: () -> Unit = {}): Boolean {
    val presenter = topViewController() ?: return false
    val controller = UIActivityViewController(
        activityItems = items,
        applicationActivities = null,
    )
    controller.completionWithItemsHandler = { _, _, _, _ -> onFinished() }

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
    return true
}
