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
import kotlinx.coroutines.withContext

internal class KeyboardMonitorRouter : KeyboardMonitor {

  private val statusState = MutableStateFlow(KeyboardStatus(isVisible = false, heightPx = 0))
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val lock = Any()

  private var isRegistered = false
  private var currentAnchor: View? = null
  private var monitorJob: Job? = null

  private var isMonitoring = false
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

  override suspend fun getCurrentKeyboardStatus(): KeyboardStatus = withContext(Dispatchers.Main.immediate) {
    val anchor = synchronized(lock) {
      currentAnchor ?: ActivityLifecycleRegistry.getCurrentActivity()?.window?.decorView
    }
    if (anchor != null) {
      statusState.value = KeyboardMonitorImpl(anchor).getCurrentKeyboardStatus()
    } else {
      statusState.value = KeyboardStatus(isVisible = false, heightPx = 0)
    }
    statusState.value
  }

  private fun onObserverStart() {
    synchronized(lock) {
      observerCount += 1
      registerAnchorListenerIfNeeded()
      if (!isMonitoring) {
        startMonitoringInternalLocked()
      }
    }
  }

  private fun onObserverStop() {
    synchronized(lock) {
      observerCount = (observerCount - 1).coerceAtLeast(0)
      if (observerCount == 0 && isMonitoring) {
        stopMonitoringInternalLocked()
      }
      if (observerCount == 0) {
        unregisterAnchorListenerIfNeeded()
      }
    }
  }

  private fun shouldMonitorLocked(): Boolean {
    return observerCount > 0
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
    monitorJob = scope.launch {
      monitor.observeKeyboardStatus().collect { status ->
        statusState.value = status
      }
    }
    isMonitoring = true
  }

  private fun stopMonitoringInternalLocked() {
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
