package com.airsaid.toolkit

import android.annotation.SuppressLint
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Android implementation of [HapticFeedback].
 */
internal class HapticFeedbackImpl : HapticFeedback {

  override fun perform(type: HapticFeedbackType): Boolean {
    val anchor = ActivityLifecycleRegistry.getCurrentActivity()?.window?.decorView
    return anchor?.performHapticFeedback(type) ?: false
  }
}

/**
 * Performs semantic haptic feedback using Android's View-based haptics API.
 */
fun View.performHapticFeedback(type: HapticFeedbackType): Boolean {
  return performHapticFeedback(hapticFeedbackConstantFor(type))
}

@SuppressLint("InlinedApi")
internal fun hapticFeedbackConstantFor(
  type: HapticFeedbackType,
  sdkInt: Int = Build.VERSION.SDK_INT,
): Int {
  return when (type) {
    HapticFeedbackType.SELECTION -> if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      HapticFeedbackConstants.SEGMENT_TICK
    } else {
      HapticFeedbackConstants.CLOCK_TICK
    }
    HapticFeedbackType.SUCCESS -> if (sdkInt >= Build.VERSION_CODES.Q) {
      HapticFeedbackConstants.CONFIRM
    } else {
      HapticFeedbackConstants.CONTEXT_CLICK
    }
    HapticFeedbackType.WARNING,
    HapticFeedbackType.ERROR,
    -> if (sdkInt >= Build.VERSION_CODES.Q) {
      HapticFeedbackConstants.REJECT
    } else {
      HapticFeedbackConstants.LONG_PRESS
    }
  }
}
