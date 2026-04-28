package com.airsaid.toolkit

/**
 * iOS implementation of [AppLifecycleMonitorFactory].
 */
internal actual object AppLifecycleMonitorFactory {

  /**
   * Creates a [AppLifecycleMonitor] instance for iOS.
   */
  actual fun create(): AppLifecycleMonitor {
    return AppLifecycleMonitorImpl()
  }
}
