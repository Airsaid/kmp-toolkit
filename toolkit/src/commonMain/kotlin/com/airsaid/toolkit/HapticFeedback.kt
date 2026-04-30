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
   * Performs haptic feedback for the given [type].
   *
   * @return True if the request was accepted by the platform. This does not guarantee
   * that the user felt haptic feedback.
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
