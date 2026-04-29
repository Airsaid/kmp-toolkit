package com.airsaid.toolkit

/**
 * Supported platforms for the toolkit module.
 */
sealed interface PlatformType {

  /**
   * Android platform.
   */
  data object Android : PlatformType

  /**
   * iOS platform.
   */
  data object Ios : PlatformType
}

/**
 * Returns true when this platform is Android.
 */
fun PlatformType.isAndroid(): Boolean = this is PlatformType.Android

/**
 * Returns true when this platform is iOS.
 */
fun PlatformType.isIos(): Boolean = this is PlatformType.Ios

/**
 * Platform accessor for common code.
 */
internal expect object Platform {

  /**
   * Current platform.
   */
  val current: PlatformType
}
