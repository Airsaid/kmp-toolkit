package com.airsaid.toolkit

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * Android implementation of [AppLifecycleMonitor].
 */
internal class AppLifecycleMonitorImpl(
  private val application: Application,
) : AppLifecycleMonitor {

  private val tracker = AppLifecycleStateTracker()
  private val statusState = MutableStateFlow(tracker.currentStatus)
  private val lock = Any()

  private var callback: Application.ActivityLifecycleCallbacks? = null
  private var isMonitoring = false
  private var isManuallyStarted = false
  private var isExplicitlyStopped = false
  private var observerCount = 0

  private var startedCount = 0
  private var resumedCount = 0

  override fun observeAppLifecycle(): Flow<AppLifecycleStatus> {
    return statusState
      .onStart { onObserverStart() }
      .onCompletion { onObserverStop() }
      .conflate()
      .distinctUntilChanged()
  }

  override suspend fun getCurrentStatus(): AppLifecycleStatus {
    return statusState.value
  }

  override fun startMonitoring() {
    synchronized(lock) {
      isExplicitlyStopped = false
      isManuallyStarted = true
      if (!isMonitoring) {
        startMonitoringInternal()
      }
    }
  }

  override fun stopMonitoring() {
    synchronized(lock) {
      isManuallyStarted = false
      isExplicitlyStopped = true
      if (isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun onObserverStart() {
    synchronized(lock) {
      observerCount += 1
      if (!isExplicitlyStopped && !isMonitoring) {
        startMonitoringInternal()
      }
    }
  }

  private fun onObserverStop() {
    synchronized(lock) {
      observerCount = (observerCount - 1).coerceAtLeast(0)
      if (observerCount == 0 && !isManuallyStarted && isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun startMonitoringInternal() {
    if (isMonitoring) return

    ActivityLifecycleRegistry.initialize(application)
    val newCallback = object : Application.ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

      override fun onActivityStarted(activity: Activity) {
        startedCount += 1
        updateStatus()
      }

      override fun onActivityResumed(activity: Activity) {
        resumedCount += 1
        updateStatus()
      }

      override fun onActivityPaused(activity: Activity) {
        resumedCount = (resumedCount - 1).coerceAtLeast(0)
        updateStatus()
      }

      override fun onActivityStopped(activity: Activity) {
        startedCount = (startedCount - 1).coerceAtLeast(0)
        updateStatus()
      }

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

      override fun onActivityDestroyed(activity: Activity) = Unit
    }

    callback = newCallback
    ActivityLifecycleRegistry.register(newCallback)
    syncInitialState()
    isMonitoring = true
  }

  private fun stopMonitoringInternal() {
    val currentCallback = callback ?: return
    ActivityLifecycleRegistry.unregister(currentCallback)
    callback = null
    isMonitoring = false
  }

  private fun updateStatus() {
    val isForeground = resumedCount > 0
    val isVisible = startedCount > 0
    statusState.value = tracker.update(
      isInForeground = isForeground,
      isVisible = isVisible,
    )
  }

  private fun syncInitialState() {
    val state = ProcessLifecycleOwner.get().lifecycle.currentState
    startedCount = if (state.isAtLeast(Lifecycle.State.STARTED)) 1 else 0
    resumedCount = if (state.isAtLeast(Lifecycle.State.RESUMED)) 1 else 0
    updateStatus()
  }
}
