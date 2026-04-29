package com.airsaid.toolkit

import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

internal class KeyboardMonitorRouter : KeyboardMonitor {

  private val statusState = MutableStateFlow(KeyboardStatus(isVisible = false, heightPx = 0))
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val lock = Any()

  private var isRegistered = false
  private var currentAnchor: View? = null
  private var currentMonitor: KeyboardMonitor? = null
  private var monitorJob: Job? = null

  private var isMonitoring = false
  private var isManuallyStarted = false
  private var isExplicitlyStopped = false
  private var observerCount = 0

  private val anchorListener: (KeyboardAnchorEvent) -> Unit = { event ->
    onAnchorEvent(event)
  }

  override fun observeKeyboardStatus(): Flow<KeyboardStatus> {
    return statusState
      .onStart { onObserverStart() }
      .onCompletion { onObserverStop() }
      .conflate()
      .distinctUntilChanged()
  }

  override suspend fun getCurrentStatus(): KeyboardStatus {
    return statusState.value
  }

  override fun startMonitoring() {
    synchronized(lock) {
      isExplicitlyStopped = false
      isManuallyStarted = true
      registerAnchorListenerIfNeeded()
      if (!isMonitoring) {
        startMonitoringInternalLocked()
      }
    }
  }

  override fun stopMonitoring() {
    synchronized(lock) {
      isManuallyStarted = false
      isExplicitlyStopped = true
      if (isMonitoring) {
        stopMonitoringInternalLocked()
      }
      if (observerCount == 0) {
        unregisterAnchorListenerIfNeeded()
      }
    }
  }

  private fun onObserverStart() {
    synchronized(lock) {
      observerCount += 1
      registerAnchorListenerIfNeeded()
      if (!isExplicitlyStopped && !isMonitoring) {
        startMonitoringInternalLocked()
      }
    }
  }

  private fun onObserverStop() {
    synchronized(lock) {
      observerCount = (observerCount - 1).coerceAtLeast(0)
      if (observerCount == 0 && !isManuallyStarted && isMonitoring) {
        stopMonitoringInternalLocked()
      }
      if (observerCount == 0 && !isManuallyStarted) {
        unregisterAnchorListenerIfNeeded()
      }
    }
  }

  private fun shouldMonitorLocked(): Boolean {
    return !isExplicitlyStopped && (isManuallyStarted || observerCount > 0)
  }

  private fun onAnchorEvent(event: KeyboardAnchorEvent) {
    when (event) {
      is KeyboardAnchorEvent.Available -> onAnchorAvailable(event.view)
      is KeyboardAnchorEvent.Unavailable -> onAnchorUnavailable(event.view)
    }
  }

  private fun onAnchorAvailable(view: View) {
    synchronized(lock) {
      if (currentAnchor === view) return
      currentAnchor = view
      if (shouldMonitorLocked()) {
        if (isMonitoring) {
          stopMonitoringInternalLocked()
        }
        startMonitoringInternalLocked()
      }
    }
  }

  private fun onAnchorUnavailable(view: View) {
    synchronized(lock) {
      if (currentAnchor !== view) return
      currentAnchor = null
      if (isMonitoring) {
        stopMonitoringInternalLocked()
      }
      statusState.value = KeyboardStatus(isVisible = false, heightPx = 0)
    }
  }

  private fun startMonitoringInternalLocked() {
    if (isMonitoring) return
    val view = currentAnchor
    if (view == null) {
      statusState.value = KeyboardStatus(isVisible = false, heightPx = 0)
      return
    }
    val monitor = KeyboardMonitorImpl(view)
    currentMonitor = monitor
    monitor.startMonitoring()
    monitorJob = scope.launch {
      monitor.observeKeyboardStatus().collect { status ->
        statusState.value = status
      }
    }
    isMonitoring = true
  }

  private fun stopMonitoringInternalLocked() {
    currentMonitor?.stopMonitoring()
    currentMonitor = null
    monitorJob?.cancel()
    monitorJob = null
    isMonitoring = false
  }

  private fun registerAnchorListenerIfNeeded() {
    if (isRegistered) return
    KeyboardAnchorRegistry.register(anchorListener)
    currentAnchor = ActivityLifecycleRegistry.getCurrentActivity()?.window?.decorView
    isRegistered = true
    if (currentAnchor == null) {
      statusState.value = KeyboardStatus(isVisible = false, heightPx = 0)
    }
  }

  private fun unregisterAnchorListenerIfNeeded() {
    if (!isRegistered) return
    KeyboardAnchorRegistry.unregister(anchorListener)
    isRegistered = false
    currentAnchor = null
    statusState.value = KeyboardStatus(isVisible = false, heightPx = 0)
  }
}
