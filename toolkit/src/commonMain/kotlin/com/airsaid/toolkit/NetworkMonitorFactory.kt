package com.airsaid.toolkit

/**
 * Factory object for creating [NetworkMonitor] instances.
 *
 * Each platform should provide its own actual implementation.
 *
 * @author airsaid
 */
internal expect object NetworkMonitorFactory {

  /**
   * Creates a platform-specific [NetworkMonitor] instance.
   */
  fun create(): NetworkMonitor
}
