package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectGetHeight
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSLock
import platform.Foundation.NSValue
import platform.UIKit.UIKeyboardFrameEndUserInfoKey
import platform.UIKit.UIKeyboardWillChangeFrameNotification
import platform.UIKit.UIKeyboardWillHideNotification
import platform.UIKit.UIKeyboardWillShowNotification
import platform.UIKit.UIScreen
import platform.UIKit.CGRectValue

/**
 * iOS implementation of [KeyboardMonitor].
 */
internal class KeyboardMonitorImpl : KeyboardMonitor {

  private val statusState = MutableStateFlow(KeyboardStatus(isVisible = false, heightPx = 0))
  private val lock = NSLock()
  private val observers = mutableListOf<Any>()

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

    observers += center.addObserverForName(
      name = UIKeyboardWillShowNotification,
      `object` = null,
      queue = null,
      usingBlock = { notification: NSNotification? ->
        handleKeyboardNotification(notification, isVisible = true)
      },
    )
    observers += center.addObserverForName(
      name = UIKeyboardWillChangeFrameNotification,
      `object` = null,
      queue = null,
      usingBlock = { notification: NSNotification? ->
        handleKeyboardNotification(notification, isVisible = true)
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
  }

  private fun handleKeyboardNotification(
    notification: NSNotification?,
    isVisible: Boolean,
  ) {
    val heightPx = notification?.userInfo?.get(UIKeyboardFrameEndUserInfoKey)
      ?.let { value ->
        (value as? NSValue)?.let { parseKeyboardHeightPx(it) }
      } ?: 0
    updateStatus(isVisible = isVisible, heightPx = heightPx)
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun parseKeyboardHeightPx(value: NSValue): Int {
    val rect = value.CGRectValue()
    val heightPoints = CGRectGetHeight(rect)
    val scale = UIScreen.mainScreen.scale
    return (heightPoints * scale).toInt()
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
