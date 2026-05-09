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
 * @property maxReportLatencyMillis Maximum delivery latency for batched events when supported.
 * Platforms without sensor batching ignore this value.
 */
data class SensorOptions(
  val samplingRateHz: Int? = null,
  val delay: SensorDelay = SensorDelay.NORMAL,
  val maxReportLatencyMillis: Long = 0,
) {

  init {
    require(samplingRateHz == null || samplingRateHz > 0) {
      "samplingRateHz must be greater than 0."
    }
    require(maxReportLatencyMillis >= 0) {
      "maxReportLatencyMillis must be greater than or equal to 0."
    }
  }
}
