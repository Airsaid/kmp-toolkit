package com.airsaid.toolkit

/**
 * Android implementation of [Platform].
 */
actual object Platform {

  /**
   * Current platform type.
   */
  actual val type: PlatformType = PlatformType.ANDROID
}
