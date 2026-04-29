package com.airsaid.toolkit

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle

/**
 * Android implementation of [KeyboardMonitorFactory].
 */
internal actual object KeyboardMonitorFactory {

  private var isInitialized = false
  private var isRegistered = false

  /**
   * Initializes the factory with an Android [Context].
   * If the context is an [Activity], its decorView will be used as the anchor.
   */
  internal fun initialize(context: Context) {
    val app = context.applicationContext as? Application
      ?: throw IllegalStateException(
        "KeyboardMonitorFactory requires an Application context on Android."
      )
    ActivityLifecycleRegistry.initialize(app)
    registerCallbacksIfNeeded()
    if (context is Activity) {
      KeyboardAnchorRegistry.updateAnchor(context.window.decorView)
    }
    isInitialized = true
  }

  /**
   * Creates a [KeyboardMonitor] instance using the previously initialized view.
   */
  actual fun create(): KeyboardMonitor {
    if (!isInitialized) {
      throw IllegalStateException(
        "KeyboardMonitorFactory must be initialized with Context on Android. " +
            "Call Toolkit.initialize(context) first."
      )
    }
    return KeyboardMonitorRouter()
  }

  private fun registerCallbacksIfNeeded() {
    if (isRegistered) return
    ActivityLifecycleRegistry.register(KeyboardAnchorLifecycleCallbacks)
    isRegistered = true
  }
}

private object KeyboardAnchorLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

  override fun onActivityStarted(activity: Activity) = Unit

  override fun onActivityResumed(activity: Activity) {
    KeyboardAnchorRegistry.updateAnchor(activity.window.decorView)
  }

  override fun onActivityPaused(activity: Activity) {
    val currentAnchor = KeyboardAnchorRegistry.getAnchor()
    if (currentAnchor === activity.window.decorView) {
      KeyboardAnchorRegistry.updateAnchor(null)
    }
  }

  override fun onActivityStopped(activity: Activity) = Unit

  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

  override fun onActivityDestroyed(activity: Activity) = Unit
}
