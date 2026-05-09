package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow

/**
 * Represents the current keyboard status.
 *
 * @property isVisible True when the keyboard is visible.
 * @property heightPx Visible keyboard overlap height in platform pixels. This is `0` when
 * the keyboard is hidden.
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
   *
   * Monitoring starts automatically while this flow is collected and stops when there are
   * no active collectors.
   */
  fun observeKeyboardStatus(): Flow<KeyboardStatus>

  /**
   * Retrieves the latest known keyboard status.
   */
  suspend fun getCurrentKeyboardStatus(): KeyboardStatus
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

internal fun resolveKeyboardStatus(
  isVisible: Boolean,
  heightPx: Int,
): KeyboardStatus {
  val resolvedVisible = isVisible && heightPx > 0
  return KeyboardStatus(
    isVisible = resolvedVisible,
    heightPx = if (resolvedVisible) heightPx else 0,
  )
}
