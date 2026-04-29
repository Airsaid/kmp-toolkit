package com.airsaid.toolkit

import android.content.Context

/**
 * Android implementation of [NetworkMonitorFactory].
 *
 * Provides a way to create [NetworkMonitor] instances for Android.
 *
 * @author airsaid
 */
internal actual object NetworkMonitorFactory {

  private var applicationContext: Context? = null

  /**
   * Must be initialized with an Android [Context] before creating [NetworkMonitor] instances.
   *
   * Typically called in [android.app.Application] or [android.app.Activity] during app startup.
   *
   * @param context The Android context to be used for network monitoring.
   */
  internal fun initialize(context: Context) {
    applicationContext = context.applicationContext
  }

  /**
   * Creates a [NetworkMonitor] instance using the previously initialized context.
   *
   * @return A [NetworkMonitor] implementation for Android.
   * @throws IllegalStateException If [initialize] has not been called prior to creation.
   */
  actual fun create(): NetworkMonitor {
    val context = applicationContext
      ?: throw IllegalStateException(
        "NetworkMonitorFactory must be initialized with Context on Android. " +
            "Call Toolkit.initialize(context) first."
      )
    return NetworkMonitorImpl(context)
  }
}
