package com.airsaid.toolkit

/**
 * iOS implementation of [HapticFeedbackFactory].
 */
internal actual object HapticFeedbackFactory {

  /**
   * Creates a [HapticFeedback] instance for iOS.
   */
  actual fun create(): HapticFeedback {
    return HapticFeedbackImpl()
  }
}
