package com.airsaid.toolkit

/**
 * Supported haptic feedback types.
 */
enum class HapticFeedbackType {
  SUCCESS,
  WARNING,
  ERROR,
  SELECTION,
}

/**
 * Provides haptic feedback capabilities.
 */
interface HapticFeedback {

  /**
   * Returns true when the current device supports haptics.
   */
  fun isSupported(): Boolean

  /**
   * Performs haptic feedback for the given [type].
   *
   * @return True if the request was handled.
   */
  fun perform(type: HapticFeedbackType): Boolean
}

/**
 * Factory object for creating [HapticFeedback] instances.
 */
internal expect object HapticFeedbackFactory {

  /**
   * Creates a platform-specific [HapticFeedback] instance.
   */
  fun create(): HapticFeedback
}
