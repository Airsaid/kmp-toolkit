package com.airsaid.toolkit

import platform.CoreHaptics.CHHapticEngine
import platform.Foundation.NSThread
import platform.UIKit.UISelectionFeedbackGenerator
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation of [HapticFeedback].
 */
internal class HapticFeedbackImpl : HapticFeedback {

  private var selectionGenerator: UISelectionFeedbackGenerator? = null
  private var notificationGenerator: UINotificationFeedbackGenerator? = null

  override fun perform(type: HapticFeedbackType): Boolean {
    if (!CHHapticEngine.capabilitiesForHardware().supportsHaptics) return false

    if (NSThread.isMainThread) {
      performOnMainThread(type)
    } else {
      dispatch_async(dispatch_get_main_queue()) {
        performOnMainThread(type)
      }
    }
    return true
  }

  private fun performOnMainThread(type: HapticFeedbackType) {
    when (type) {
      HapticFeedbackType.SELECTION -> {
        val generator = selectionGenerator ?: UISelectionFeedbackGenerator().also {
          selectionGenerator = it
        }
        generator.prepare()
        generator.selectionChanged()
      }
      HapticFeedbackType.SUCCESS -> {
        val generator = notificationGenerator()
        generator.prepare()
        generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
      }
      HapticFeedbackType.WARNING -> {
        val generator = notificationGenerator()
        generator.prepare()
        generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
      }
      HapticFeedbackType.ERROR -> {
        val generator = notificationGenerator()
        generator.prepare()
        generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
      }
    }
  }

  private fun notificationGenerator(): UINotificationFeedbackGenerator {
    return notificationGenerator ?: UINotificationFeedbackGenerator().also {
      notificationGenerator = it
    }
  }
}
