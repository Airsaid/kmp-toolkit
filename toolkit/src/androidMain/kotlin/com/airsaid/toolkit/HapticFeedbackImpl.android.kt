package com.airsaid.toolkit

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Android implementation of [HapticFeedback].
 */
internal class HapticFeedbackImpl(
  context: Context,
) : HapticFeedback {

  private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    manager.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
  }

  override fun isSupported(): Boolean {
    return vibrator.hasVibrator()
  }

  override fun perform(type: HapticFeedbackType): Boolean {
    if (!isSupported()) return false

    val pattern = buildLegacyPattern(type)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val effect = createPredefinedEffect(type)
        ?: VibrationEffect.createWaveform(pattern, NO_REPEAT)
      vibrator.vibrate(effect)
      return true
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(VibrationEffect.createWaveform(pattern, NO_REPEAT))
      return true
    }

    @Suppress("DEPRECATION")
    vibrator.vibrate(pattern, NO_REPEAT)
    return true
  }
}

private const val NO_REPEAT = -1

private fun buildLegacyPattern(type: HapticFeedbackType): LongArray {
  return when (type) {
    HapticFeedbackType.SELECTION -> longArrayOf(0L, 20L)
    HapticFeedbackType.SUCCESS -> longArrayOf(0L, 30L)
    HapticFeedbackType.WARNING -> longArrayOf(0L, 30L, 40L, 30L)
    HapticFeedbackType.ERROR -> longArrayOf(0L, 50L, 50L, 50L)
  }
}

@SuppressLint("NewApi")
private fun createPredefinedEffect(type: HapticFeedbackType): VibrationEffect? {
  return when (type) {
    HapticFeedbackType.SELECTION -> VibrationEffect.createPredefined(
      VibrationEffect.EFFECT_TICK,
    )
    HapticFeedbackType.SUCCESS -> VibrationEffect.createPredefined(
      VibrationEffect.EFFECT_CLICK,
    )
    HapticFeedbackType.WARNING -> VibrationEffect.createPredefined(
      VibrationEffect.EFFECT_DOUBLE_CLICK,
    )
    HapticFeedbackType.ERROR -> VibrationEffect.createPredefined(
      VibrationEffect.EFFECT_HEAVY_CLICK,
    )
  }
}
