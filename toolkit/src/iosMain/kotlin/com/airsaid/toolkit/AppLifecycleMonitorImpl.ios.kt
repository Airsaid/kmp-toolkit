package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
@Suppress("DEPRECATION")
internal class AppLifecycleMonitorImpl : AppLifecycleMonitor {

  private val tracker = AppLifecycleStateTracker()
  private val statusState = MutableStateFlow(tracker.currentStatus)
  private val startEvents = MutableSharedFlow<AppStartType>(
    replay = 0,
    extraBufferCapacity = 1,
  )
  private val lock = NSLock()
  private val observers = mutableListOf<NSObjectProtocol>()

  private var isMonitoring = false
  private var isManuallyStarted = false
  private var isSyncingInitialState = false
  private var observerCount = 0

  override fun observeAppLifecycle(): Flow<AppLifecycleStatus> {
    return statusState
      .onStart { onObserverStart() }
      .onCompletion { onObserverStop() }
  }

  override fun observeAppStartEvents(): Flow<AppStartType> {
    return startEvents
      .onStart { onObserverStart() }
      .onCompletion { onObserverStop() }
  }

  override suspend fun getCurrentStatus(): AppLifecycleStatus {
    withLock {
      syncInitialState()
      return statusState.value
    }
  }

  @Deprecated(
    message = "Lifecycle monitoring now starts automatically while observeAppLifecycle() or " +
      "observeAppStartEvents() is collected.",
  )
  override fun startMonitoring() {
    withLock {
      isManuallyStarted = true
      if (!isMonitoring) {
        startMonitoringInternal()
      }
    }
  }

  @Deprecated(
    message = "Lifecycle monitoring now stops automatically when observeAppLifecycle() and " +
      "observeAppStartEvents() have no collectors.",
  )
  override fun stopMonitoring() {
    withLock {
      isManuallyStarted = false
      if (observerCount == 0 && isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun onObserverStart() {
    withLock {
      observerCount += 1
      if (!isMonitoring) {
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

    isSyncingInitialState = true
    try {
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
      isMonitoring = true
      syncInitialState()
    } finally {
      isSyncingInitialState = false
    }
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
    val (isForeground, isVisible) = resolveCurrentState()
    val currentStatus = tracker.currentStatus
    if (currentStatus.isInForeground == isForeground &&
      currentStatus.isVisible == isVisible
    ) {
      return
    }
    val update = tracker.update(
      isInForeground = isForeground,
      isVisible = isVisible,
    )
    publishUpdate(update, emitStartEvent = false)
  }

  private fun updateStatus(
    isInForeground: Boolean,
    isVisible: Boolean,
  ) {
    withLock {
      val update = tracker.update(
        isInForeground = isInForeground,
        isVisible = isVisible,
      )
      publishUpdate(update, emitStartEvent = !isSyncingInitialState)
    }
  }

  private fun publishUpdate(
    update: AppLifecycleUpdate,
    emitStartEvent: Boolean,
  ) {
    statusState.value = update.status
    if (emitStartEvent) {
      update.startType?.let { startEvents.tryEmit(it) }
    }
    if (update.status.isFirstLaunch) {
      tracker.clearFirstLaunchFlag()
      statusState.value = tracker.currentStatus
    }
  }

  private inline fun <T> withLock(block: () -> T): T {
    lock.lock()
    try {
      return block()
    } finally {
      lock.unlock()
    }
  }

  private fun resolveCurrentState(): Pair<Boolean, Boolean> {
    val appState = UIApplication.sharedApplication.applicationState
    val isForeground = appState == UIApplicationState.UIApplicationStateActive
    val isVisible = appState != UIApplicationState.UIApplicationStateBackground
    return isForeground to isVisible
  }
}
