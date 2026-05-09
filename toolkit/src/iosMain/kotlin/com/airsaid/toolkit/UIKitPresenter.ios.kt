package com.airsaid.toolkit

import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController

internal fun resolvePresenter(): UIViewController? {
  val keyWindow = resolveKeyWindow() ?: return null
  return topViewController(keyWindow.rootViewController)
}

internal fun resolveKeyWindow(): UIWindow? {
  val application = UIApplication.sharedApplication
  return application.keyWindow ?: firstKeyWindow(application.windows)
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
