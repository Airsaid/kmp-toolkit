package com.airsaid.toolkit

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

    ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
      updateStatus(insets)
      insets
    }
    ViewCompat.getRootWindowInsets(view)?.let(::updateStatus)
    ViewCompat.requestApplyInsets(view)
    isMonitoring = true
  }

  private fun stopMonitoringInternal() {
    ViewCompat.setOnApplyWindowInsetsListener(view, null)
    isMonitoring = false
    statusState.value = KeyboardStatus(isVisible = false, heightPx = 0)
  }

  private fun updateStatus(insets: WindowInsetsCompat) {
    val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
    statusState.value = resolveKeyboardStatus(
      isVisible = insets.isVisible(WindowInsetsCompat.Type.ime()),
      heightPx = imeHeight,
      thresholdPx = thresholdPx,
    )
  }

  companion object {
    private const val VISIBILITY_THRESHOLD_DP = 100f
  }
}
