package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.darwin.NSObjectProtocol
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSLock
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

/**
 * iOS implementation of [AppLifecycleMonitor].
 */
internal class AppLifecycleMonitorImpl : AppLifecycleMonitor {

  private val tracker = AppLifecycleStateTracker()
  private val statusState = MutableStateFlow(tracker.currentStatus)
  private val lock = NSLock()
  private val observers = mutableListOf<NSObjectProtocol>()

  private var isMonitoring = false
  private var isManuallyStarted = false
  private var isExplicitlyStopped = false
  private var observerCount = 0

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
    withLock {
      isExplicitlyStopped = false
      isManuallyStarted = true
      if (!isMonitoring) {
        startMonitoringInternal()
      }
    }
  }

  override fun stopMonitoring() {
    withLock {
      isManuallyStarted = false
      isExplicitlyStopped = true
      if (isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun onObserverStart() {
    withLock {
      observerCount += 1
      if (!isExplicitlyStopped && !isMonitoring) {
        startMonitoringInternal()
      }
    }
  }

  private fun onObserverStop() {
    withLock {
      observerCount = (observerCount - 1).coerceAtLeast(0)
      if (observerCount == 0 && !isManuallyStarted && isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun startMonitoringInternal() {
    if (isMonitoring) return

    val center = NSNotificationCenter.defaultCenter
    val queue = NSOperationQueue.mainQueue

    observers += center.addObserverForName(
      name = UIApplicationDidBecomeActiveNotification,
      `object` = null,
      queue = queue,
      usingBlock = { _: NSNotification? ->
        updateStatus(isInForeground = true, isVisible = true)
      },
    )
    observers += center.addObserverForName(
      name = UIApplicationWillResignActiveNotification,
      `object` = null,
      queue = queue,
      usingBlock = { _: NSNotification? ->
        updateStatus(isInForeground = false, isVisible = true)
      },
    )
    observers += center.addObserverForName(
      name = UIApplicationDidEnterBackgroundNotification,
      `object` = null,
      queue = queue,
      usingBlock = { _: NSNotification? ->
        updateStatus(isInForeground = false, isVisible = false)
      },
    )
    observers += center.addObserverForName(
      name = UIApplicationWillEnterForegroundNotification,
      `object` = null,
      queue = queue,
      usingBlock = { _: NSNotification? ->
        updateStatus(isInForeground = false, isVisible = true)
      },
    )

    syncInitialState()
    isMonitoring = true
  }

  private fun stopMonitoringInternal() {
    val center = NSNotificationCenter.defaultCenter
    observers.forEach { observer ->
      center.removeObserver(observer)
    }
    observers.clear()
    isMonitoring = false
  }

  private fun syncInitialState() {
    val appState = UIApplication.sharedApplication.applicationState
    val isForeground = appState == UIApplicationState.UIApplicationStateActive
    val isVisible = appState != UIApplicationState.UIApplicationStateBackground
    updateStatus(isInForeground = isForeground, isVisible = isVisible)
  }

  private fun updateStatus(
    isInForeground: Boolean,
    isVisible: Boolean,
  ) {
    statusState.value = tracker.update(
      isInForeground = isInForeground,
      isVisible = isVisible,
    )
  }

  private inline fun <T> withLock(block: () -> T): T {
    lock.lock()
    try {
      return block()
    } finally {
      lock.unlock()
    }
  }
}
