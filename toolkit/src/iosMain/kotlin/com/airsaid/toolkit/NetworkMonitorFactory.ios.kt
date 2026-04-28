package com.airsaid.toolkit

/**
 * iOS implementation of [NetworkMonitorFactory].
 *
 * Creates [NetworkMonitor] instances for iOS using Network framework.
 *
 * @author airsaid
 */
internal actual object NetworkMonitorFactory {

  /**
   * Creates a platform-specific [NetworkMonitor] instance for iOS.
   *
   * @return A [NetworkMonitor] implementation backed by [NetworkMonitorImpl].
   */
  actual fun create(): NetworkMonitor {
    return NetworkMonitorImpl()
  }
}
