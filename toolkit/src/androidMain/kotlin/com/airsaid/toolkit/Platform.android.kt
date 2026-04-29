package com.airsaid.toolkit

/**
 * Android implementation of [Platform].
 */
actual object Platform {

  /**
   * Current platform.
   */
  actual val current: PlatformType = PlatformType.Android
}
