package com.airsaid.toolkit

/**
 * iOS implementation of [KeyboardMonitorFactory].
 */
internal actual object KeyboardMonitorFactory {

  /**
   * Creates a [KeyboardMonitor] instance for iOS.
   */
  actual fun create(): KeyboardMonitor {
    return KeyboardMonitorImpl()
  }
}
