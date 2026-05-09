package com.airsaid.toolkit

import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

/**
 * Android implementation of [KeyboardMonitor].
 */
internal class KeyboardMonitorImpl(
  private val view: View,
) : KeyboardMonitor {

  private val statusState = MutableStateFlow(KeyboardStatus(isVisible = false, heightPx = 0))
  private val lock = Any()
  private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
    updateStatusFromRootInsets()
  }
  private val attachListener = object : View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View) {
      addLayoutListener()
      updateStatusFromRootInsets()
      ViewCompat.requestApplyInsets(v)
    }

    override fun onViewDetachedFromWindow(v: View) {
      removeLayoutListener()
      statusState.value = KeyboardStatus(isVisible = false, heightPx = 0)
    }
  }

  private var isMonitoring = false
  private var observerCount = 0
  private var isLayoutListenerAdded = false

  override fun observeKeyboardStatus(): Flow<KeyboardStatus> {
    return statusState
      .onStart { onObserverStart() }
      .onCompletion { onObserverStop() }
      .conflate()
      .distinctUntilChanged()
  }

  override suspend fun getCurrentKeyboardStatus(): KeyboardStatus = withContext(Dispatchers.Main.immediate) {
    updateStatusFromRootInsets()
    statusState.value
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
      if (observerCount == 0 && isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun startMonitoringInternal() {
    if (isMonitoring) return

    view.addOnAttachStateChangeListener(attachListener)
    if (view.isAttachedToWindow) {
      addLayoutListener()
      updateStatusFromRootInsets()
    } else {
      statusState.value = KeyboardStatus(isVisible = false, heightPx = 0)
    }
    ViewCompat.requestApplyInsets(view)
    isMonitoring = true
  }

  private fun stopMonitoringInternal() {
    view.removeOnAttachStateChangeListener(attachListener)
    removeLayoutListener()
    isMonitoring = false
    statusState.value = KeyboardStatus(isVisible = false, heightPx = 0)
  }

  private fun addLayoutListener() {
    if (isLayoutListenerAdded) return
    view.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    isLayoutListenerAdded = true
  }

  private fun removeLayoutListener() {
    if (!isLayoutListenerAdded) return
    val observer = view.viewTreeObserver
    if (observer.isAlive) {
      observer.removeOnGlobalLayoutListener(layoutListener)
    }
    isLayoutListenerAdded = false
  }

  private fun updateStatusFromRootInsets() {
    val insets = ViewCompat.getRootWindowInsets(view)
    if (insets == null) {
      statusState.value = KeyboardStatus(isVisible = false, heightPx = 0)
      return
    }
    updateStatus(insets)
  }

  private fun updateStatus(insets: WindowInsetsCompat) {
    val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
    statusState.value = resolveKeyboardStatus(
      isVisible = insets.isVisible(WindowInsetsCompat.Type.ime()),
      heightPx = imeHeight,
    )
  }
}
