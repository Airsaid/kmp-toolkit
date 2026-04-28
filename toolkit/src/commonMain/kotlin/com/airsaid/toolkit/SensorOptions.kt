package com.airsaid.toolkit

/**
 * Predefined sensor delay presets.
 */
enum class SensorDelay {
  FASTEST,
  GAME,
  UI,
  NORMAL,
}

/**
 * Options for sensor observation.
 *
 * @property samplingRateHz Preferred sampling rate in Hz. When set, overrides [delay].
 * @property delay Fallback delay preset when [samplingRateHz] is null.
 * @property batchEnabled Whether batching is preferred when supported.
 */
data class SensorOptions(
  val samplingRateHz: Int? = null,
  val delay: SensorDelay = SensorDelay.NORMAL,
  val batchEnabled: Boolean = false,
)
