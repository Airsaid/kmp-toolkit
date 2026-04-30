package com.airsaid.toolkit

import android.content.Context

/**
 * Android implementation of [HapticFeedbackFactory].
 */
internal actual object HapticFeedbackFactory {

  private var applicationContext: Context? = null

  /**
   * Must be initialized with an Android [Context] before creating [HapticFeedback] instances.
   */
  internal fun initialize(context: Context) {
    applicationContext = context.applicationContext
  }

  /**
   * Creates a [HapticFeedback] instance using the previously initialized context.
   */
  actual fun create(): HapticFeedback {
    applicationContext
      ?: throw IllegalStateException(
        "HapticFeedbackFactory must be initialized with Context on Android. " +
            "Call Toolkit.initialize(context) first."
      )
    return HapticFeedbackImpl()
  }
}
