package com.airsaid.toolkit

/**
 * Supported sensor types across platforms.
 */
enum class SensorType {
  ACCELEROMETER,
  GYROSCOPE,
  MAGNETOMETER,
  BAROMETER,
  STEP_COUNTER,
  STEP_DETECTOR,
  DEVICE_MOTION,
  LIGHT,
  PROXIMITY,
  GRAVITY,
  LINEAR_ACCELERATION,
  ROTATION_VECTOR,
  GAME_ROTATION_VECTOR,
  GEOMAGNETIC_ROTATION_VECTOR,
  TILT_DETECTOR,
  SIGNIFICANT_MOTION,
  MOTION_DETECT,
  STATIONARY_DETECT,
  AMBIENT_TEMPERATURE,
  RELATIVE_HUMIDITY,
}

/**
 * Indicates the quality of a sensor reading.
 */
enum class SensorAccuracy {
  UNRELIABLE,
  LOW,
  MEDIUM,
  HIGH,
}

/**
 * Represents a single sensor reading.
 *
 * @property type The sensor type.
 * @property values Sensor values in platform-defined units.
 * @property timestampNanos Timestamp in nanoseconds.
 * @property accuracy Reading accuracy.
 */
data class SensorEvent(
  val type: SensorType,
  val values: FloatArray,
  val timestampNanos: Long,
  val accuracy: SensorAccuracy,
)

/**
 * Represents sensor availability with an optional reason.
 */
data class SensorAvailability(
  val isAvailable: Boolean,
  val reason: String? = null,
)
