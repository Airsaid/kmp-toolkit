package com.airsaid.toolkit

/**
 * iOS implementation of [SensorToolkitFactory].
 */
internal actual object SensorToolkitFactory {

  /**
   * Creates a [SensorToolkit] instance for iOS.
   */
  actual fun create(): SensorToolkit {
    return SensorToolkitImpl()
  }
}
