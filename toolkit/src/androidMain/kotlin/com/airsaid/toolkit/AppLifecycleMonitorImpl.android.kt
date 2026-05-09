package com.airsaid.toolkit

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * Android implementation of [AppLifecycleMonitor].
 */
@Suppress("DEPRECATION")
internal class AppLifecycleMonitorImpl : AppLifecycleMonitor {

  private val tracker = AppLifecycleStateTracker()
  private val statusState = MutableStateFlow(tracker.currentStatus)
  private val startEvents = MutableSharedFlow<AppStartType>(
    replay = 0,
    extraBufferCapacity = 1,
  )
  private val lock = Any()

  private var isMonitoring = false
  private var isManuallyStarted = false
  private var isSyncingInitialState = false
  private var observerCount = 0

  private val lifecycleObserver = object : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
      updateStatus(isInForeground = false, isVisible = true)
    }

    override fun onResume(owner: LifecycleOwner) {
      updateStatus(isInForeground = true, isVisible = true)
    }

    override fun onPause(owner: LifecycleOwner) {
      updateStatus(isInForeground = false, isVisible = true)
    }

    override fun onStop(owner: LifecycleOwner) {
      updateStatus(isInForeground = false, isVisible = false)
    }
  }

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
    synchronized(lock) {
      syncInitialState()
      return statusState.value
    }
  }

  @Deprecated(
    message = "Lifecycle monitoring now starts automatically while observeAppLifecycle() or " +
      "observeAppStartEvents() is collected.",
  )
  override fun startMonitoring() {
    synchronized(lock) {
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
    synchronized(lock) {
      isManuallyStarted = false
      if (observerCount == 0 && isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun onObserverStart() {
    synchronized(lock) {
      observerCount += 1
      if (!isMonitoring) {
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

    isSyncingInitialState = true
    try {
      ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
      isMonitoring = true
      syncInitialState()
    } finally {
      isSyncingInitialState = false
    }
  }

  private fun stopMonitoringInternal() {
    ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
    isMonitoring = false
  }

  private fun updateStatus(
    isInForeground: Boolean,
    isVisible: Boolean,
  ) {
    synchronized(lock) {
      val update = tracker.update(
        isInForeground = isInForeground,
        isVisible = isVisible,
      )
      publishUpdate(update, emitStartEvent = !isSyncingInitialState)
    }
  }

  private fun syncInitialState() {
    val (isInForeground, isVisible) = resolveCurrentState()
    val currentStatus = tracker.currentStatus
    if (currentStatus.isInForeground == isInForeground &&
      currentStatus.isVisible == isVisible
    ) {
      return
    }
    val update = tracker.update(
      isInForeground = isInForeground,
      isVisible = isVisible,
    )
    publishUpdate(update, emitStartEvent = false)
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

  private fun resolveCurrentState(): Pair<Boolean, Boolean> {
    val state = ProcessLifecycleOwner.get().lifecycle.currentState
    return when {
      state.isAtLeast(Lifecycle.State.RESUMED) -> true to true
      state.isAtLeast(Lifecycle.State.STARTED) -> false to true
      else -> false to false
    }
  }
}
