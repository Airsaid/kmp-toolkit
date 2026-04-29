package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow

/**
 * Represents the current keyboard status.
 *
 * @property isVisible True when the keyboard is visible.
 * @property heightPx Keyboard height in pixels.
 */
data class KeyboardStatus(
  val isVisible: Boolean,
  val heightPx: Int,
)

/**
 * Monitors soft keyboard visibility and height changes.
 */
interface KeyboardMonitor {

  /**
   * Observes keyboard status updates as a [Flow].
   */
  fun observeKeyboardStatus(): Flow<KeyboardStatus>

  /**
   * Retrieves the current keyboard status.
   */
  suspend fun getCurrentStatus(): KeyboardStatus

  /**
   * Starts monitoring keyboard changes.
   */
  fun startMonitoring()

  /**
   * Stops monitoring keyboard changes.
   */
  fun stopMonitoring()
}

/**
 * Factory object for creating [KeyboardMonitor] instances.
 */
internal expect object KeyboardMonitorFactory {

  /**
   * Creates a platform-specific [KeyboardMonitor] instance.
   */
  fun create(): KeyboardMonitor
}

internal fun resolveKeyboardVisibility(
  heightPx: Int,
  thresholdPx: Int,
): Boolean {
  return heightPx > thresholdPx
}

internal fun resolveKeyboardStatus(
  isVisible: Boolean,
  heightPx: Int,
  thresholdPx: Int,
): KeyboardStatus {
  val resolvedVisible = isVisible && resolveKeyboardVisibility(heightPx, thresholdPx)
  return KeyboardStatus(
    isVisible = resolvedVisible,
    heightPx = if (resolvedVisible) heightPx else 0,
  )
}
