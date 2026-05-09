package com.airsaid.toolkit

import android.app.Application
import android.content.Context

/**
 * Android implementation of [AppLifecycleMonitorFactory].
 */
internal actual object AppLifecycleMonitorFactory {

  private var isInitialized: Boolean = false

  /**
   * Must be initialized with an Android [Context] before creating [AppLifecycleMonitor] instances.
   */
  internal fun initialize(context: Context) {
    context.applicationContext as? Application ?: throw IllegalStateException(
      "AppLifecycleMonitorFactory requires an Application context on Android."
    )
    isInitialized = true
  }

  /**
   * Creates a [AppLifecycleMonitor] instance using the previously initialized context.
   */
  actual fun create(): AppLifecycleMonitor {
    if (!isInitialized) {
      throw IllegalStateException(
        "AppLifecycleMonitorFactory must be initialized with Context on Android. " +
          "Call Toolkit.initialize(context) first."
      )
    }
    return AppLifecycleMonitorImpl()
  }
}
