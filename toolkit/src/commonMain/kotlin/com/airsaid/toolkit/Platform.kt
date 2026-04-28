package com.airsaid.toolkit

/**
 * Supported platform types for the toolkit module.
 */
enum class PlatformType {
  ANDROID,
  IOS,
}

/**
 * Platform accessor for common code.
 */
internal expect object Platform {

  /**
   * Current platform type.
   */
  val type: PlatformType
}
