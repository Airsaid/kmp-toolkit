package com.airsaid.toolkit

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * Android implementation of [KeyboardMonitor].
 */
internal class KeyboardMonitorImpl(
  private val view: View,
) : KeyboardMonitor {

  private val thresholdPx = (view.resources.displayMetrics.density * VISIBILITY_THRESHOLD_DP).toInt()
  private val statusState = MutableStateFlow(KeyboardStatus(isVisible = false, heightPx = 0))
  private val lock = Any()

  private var listener: ViewTreeObserver.OnGlobalLayoutListener? = null
  private var isMonitoring = false
  private var isManuallyStarted = false
  private var isExplicitlyStopped = false
  private var observerCount = 0

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

    val newListener = ViewTreeObserver.OnGlobalLayoutListener {
      updateStatus()
    }
    listener = newListener
    view.viewTreeObserver.addOnGlobalLayoutListener(newListener)
    updateStatus()
    isMonitoring = true
  }

  private fun stopMonitoringInternal() {
    val currentListener = listener ?: return
    val observer = view.viewTreeObserver
    if (observer.isAlive) {
      observer.removeOnGlobalLayoutListener(currentListener)
    }
    listener = null
    isMonitoring = false
  }

  private fun updateStatus() {
    val rect = Rect()
    view.getWindowVisibleDisplayFrame(rect)
    val rootView = view.rootView
    val keyboardHeight = (rootView.height - rect.bottom).coerceAtLeast(0)
    val isVisible = resolveKeyboardVisibility(keyboardHeight, thresholdPx)
    statusState.value = KeyboardStatus(
      isVisible = isVisible,
      heightPx = if (isVisible) keyboardHeight else 0,
    )
  }

  companion object {
    private const val VISIBILITY_THRESHOLD_DP = 100f
  }
}
