package com.airsaid.toolkit

import android.app.Application
import android.content.Context

/**
 * Android implementation of [AppLifecycleMonitorFactory].
 */
internal actual object AppLifecycleMonitorFactory {

  private var application: Application? = null

  /**
   * Must be initialized with an Android [Context] before creating [AppLifecycleMonitor] instances.
   */
  internal fun initialize(context: Context) {
    val app = context.applicationContext as? Application
      ?: throw IllegalStateException(
        "AppLifecycleMonitorFactory requires an Application context on Android."
      )
    application = app
  }

  /**
   * Creates a [AppLifecycleMonitor] instance using the previously initialized context.
   */
  actual fun create(): AppLifecycleMonitor {
    val app = application
      ?: throw IllegalStateException(
        "AppLifecycleMonitorFactory must be initialized with Context on Android. " +
            "Call Toolkit.initialize(ToolkitInitializer(context)) first."
      )
    return AppLifecycleMonitorImpl(app)
  }
}
