package com.kintmin.presentation.platform

import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * UIKit 화면을 얹을 자리. 이미 떠 있는 화면 위에 얹어야 한다 — 루트에 present 하면
 * "이미 present 중" 으로 거부된다.
 */
internal fun topViewController(): UIViewController? {
    val keyWindow = UIApplication.sharedApplication.connectedScenes
        .flatMap { scene -> (scene as? UIWindowScene)?.windows.orEmpty() }
        .filterIsInstance<UIWindow>()
        .firstOrNull { window -> window.isKeyWindow() }
        ?: return null

    var controller = keyWindow.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
