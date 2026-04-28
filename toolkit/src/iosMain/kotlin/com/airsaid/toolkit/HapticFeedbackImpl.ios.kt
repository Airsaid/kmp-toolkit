package com.airsaid.toolkit

import platform.UIKit.UISelectionFeedbackGenerator
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

/**
 * iOS implementation of [HapticFeedback].
 */
internal class HapticFeedbackImpl : HapticFeedback {

  override fun isSupported(): Boolean {
    return true
  }

  override fun perform(type: HapticFeedbackType): Boolean {
    when (type) {
      HapticFeedbackType.SELECTION -> {
        val generator = UISelectionFeedbackGenerator()
        generator.prepare()
        generator.selectionChanged()
      }
      HapticFeedbackType.SUCCESS -> {
        val generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
      }
      HapticFeedbackType.WARNING -> {
        val generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
      }
      HapticFeedbackType.ERROR -> {
        val generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
      }
    }
    return true
  }
}
