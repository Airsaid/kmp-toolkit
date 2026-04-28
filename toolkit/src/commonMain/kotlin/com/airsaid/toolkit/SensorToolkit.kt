package com.airsaid.toolkit

import kotlinx.coroutines.flow.Flow

/**
 * Provides access to device sensors.
 */
interface SensorToolkit {

  /**
   * Returns whether the sensor is available on the current device.
   */
  fun isAvailable(type: SensorType): SensorAvailability

  /**
   * Observes the given [type] sensor updates.
   */
  fun observe(
    type: SensorType,
    options: SensorOptions = SensorOptions(),
  ): Flow<SensorEvent>

  /**
   * Returns the latest sensor reading if available.
   */
  suspend fun getSnapshot(type: SensorType): SensorEvent?

  /**
   * Stops monitoring the specified sensor type.
   */
  fun stop(type: SensorType)
}

/**
 * Factory object for creating [SensorToolkit] instances.
 */
internal expect object SensorToolkitFactory {

  /**
   * Creates a platform-specific [SensorToolkit] instance.
   */
  fun create(): SensorToolkit
}
