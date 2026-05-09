package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectIntersection
import platform.CoreGraphics.CGRectIsEmpty
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSLock
import platform.Foundation.NSValue
import platform.UIKit.UIKeyboardFrameEndUserInfoKey
import platform.UIKit.UIKeyboardWillChangeFrameNotification
import platform.UIKit.UIKeyboardWillHideNotification
import platform.UIKit.CGRectValue
import kotlin.math.roundToInt

/**
 * iOS implementation of [KeyboardMonitor].
 */
internal class KeyboardMonitorImpl : KeyboardMonitor {

  private val statusState = MutableStateFlow(KeyboardStatus(isVisible = false, heightPx = 0))
  private val lock = NSLock()
  private val observers = mutableListOf<Any>()

  private var isMonitoring = false
  private var observerCount = 0

  override fun observeKeyboardStatus(): Flow<KeyboardStatus> {
    return statusState
      .onStart { onObserverStart() }
      .onCompletion { onObserverStop() }
      .conflate()
      .distinctUntilChanged()
  }

  override suspend fun getCurrentKeyboardStatus(): KeyboardStatus {
    return statusState.value
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
      if (observerCount == 0 && isMonitoring) {
        stopMonitoringInternal()
      }
    }
  }

  private fun startMonitoringInternal() {
    if (isMonitoring) return

    val center = NSNotificationCenter.defaultCenter

    observers += center.addObserverForName(
      name = UIKeyboardWillChangeFrameNotification,
      `object` = null,
      queue = null,
      usingBlock = { notification: NSNotification? ->
        handleKeyboardFrameNotification(notification)
      },
    )
    observers += center.addObserverForName(
      name = UIKeyboardWillHideNotification,
      `object` = null,
      queue = null,
      usingBlock = { _: NSNotification? ->
        updateStatus(isVisible = false, heightPx = 0)
      },
    )

    isMonitoring = true
  }

  private fun stopMonitoringInternal() {
    val center = NSNotificationCenter.defaultCenter
    observers.forEach { observer ->
      center.removeObserver(observer)
    }
    observers.clear()
    isMonitoring = false
    statusState.value = KeyboardStatus(isVisible = false, heightPx = 0)
  }

  private fun handleKeyboardFrameNotification(notification: NSNotification?) {
    val heightPx = notification?.userInfo?.get(UIKeyboardFrameEndUserInfoKey)
      ?.let { value ->
        (value as? NSValue)?.let { parseKeyboardOverlapHeightPx(it) }
      } ?: 0
    updateStatus(isVisible = heightPx > 0, heightPx = heightPx)
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun parseKeyboardOverlapHeightPx(value: NSValue): Int {
    val window = resolveKeyWindow() ?: return 0
    val keyboardFrameInWindow = window.convertRect(value.CGRectValue(), fromView = null)
    val overlap = CGRectIntersection(window.bounds, keyboardFrameInWindow)
    if (CGRectIsEmpty(overlap)) return 0
    return (CGRectGetHeight(overlap) * window.screen.scale).roundToInt()
  }

  private fun updateStatus(
    isVisible: Boolean,
    heightPx: Int,
  ) {
    statusState.value = KeyboardStatus(
      isVisible = isVisible,
      heightPx = if (isVisible) heightPx else 0,
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
