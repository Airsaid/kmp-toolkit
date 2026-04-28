package com.airsaid.toolkit

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Centralized activity lifecycle dispatcher for toolkit components.
 */
internal object ActivityLifecycleRegistry {

  private val lock = Any()
  private val listeners = LinkedHashSet<Application.ActivityLifecycleCallbacks>()
  private var application: Application? = null
  private var isRegistered = false
  private var currentActivityRef: WeakReference<Activity>? = null

  private val dispatcher = object : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
      dispatch { it.onActivityCreated(activity, savedInstanceState) }
    }

    override fun onActivityStarted(activity: Activity) {
      dispatch { it.onActivityStarted(activity) }
    }

    override fun onActivityResumed(activity: Activity) {
      updateCurrentActivity(activity)
      dispatch { it.onActivityResumed(activity) }
    }

    override fun onActivityPaused(activity: Activity) {
      clearCurrentActivity(activity)
      dispatch { it.onActivityPaused(activity) }
    }

    override fun onActivityStopped(activity: Activity) {
      dispatch { it.onActivityStopped(activity) }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
      dispatch { it.onActivitySaveInstanceState(activity, outState) }
    }

    override fun onActivityDestroyed(activity: Activity) {
      clearCurrentActivity(activity)
      dispatch { it.onActivityDestroyed(activity) }
    }
  }

  internal fun initialize(context: Context) {
    val app = context.applicationContext as? Application
      ?: throw IllegalStateException(
        "ActivityLifecycleRegistry requires an Application context on Android."
      )
    synchronized(lock) {
      val currentApp = application
      if (currentApp == null) {
        application = app
      } else if (currentApp != app) {
        throw IllegalStateException(
          "ActivityLifecycleRegistry has been initialized with a different Application."
        )
      }
      if (!isRegistered) {
        app.registerActivityLifecycleCallbacks(dispatcher)
        isRegistered = true
      }
    }
  }

  internal fun register(callback: Application.ActivityLifecycleCallbacks) {
    synchronized(lock) {
      if (!listeners.add(callback)) return
    }
  }

  internal fun unregister(callback: Application.ActivityLifecycleCallbacks) {
    synchronized(lock) {
      listeners.remove(callback)
    }
  }

  internal fun getCurrentActivity(): Activity? {
    return synchronized(lock) { currentActivityRef?.get() }
  }

  private fun dispatch(block: (Application.ActivityLifecycleCallbacks) -> Unit) {
    val snapshot = synchronized(lock) { listeners.toList() }
    snapshot.forEach(block)
  }

  private fun updateCurrentActivity(activity: Activity) {
    synchronized(lock) {
      currentActivityRef = WeakReference(activity)
    }
  }

  private fun clearCurrentActivity(activity: Activity) {
    synchronized(lock) {
      val current = currentActivityRef?.get()
      if (current === activity) {
        currentActivityRef = null
      }
    }
  }
}
