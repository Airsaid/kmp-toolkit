package com.airsaid.toolkit

import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController

internal fun resolvePresenter(): UIViewController? {
  val application = UIApplication.sharedApplication
  val keyWindow = application.keyWindow ?: firstKeyWindow(application.windows)
    ?: return null
  return topViewController(keyWindow.rootViewController)
}

private fun firstKeyWindow(windows: List<*>?): UIWindow? {
  if (windows == null) return null
  return windows.firstNotNullOfOrNull { window ->
    val uiWindow = window as? UIWindow
    if (uiWindow?.isKeyWindow() == true) uiWindow else null
  }
}

private fun topViewController(controller: UIViewController?): UIViewController? {
  val presented = controller?.presentedViewController
  if (presented != null) {
    return topViewController(presented)
  }
  if (controller is UINavigationController) {
    return topViewController(controller.visibleViewController)
  }
  if (controller is UITabBarController) {
    return topViewController(controller.selectedViewController)
  }
  return controller
}
