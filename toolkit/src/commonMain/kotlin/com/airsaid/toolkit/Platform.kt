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
val PlatformType.isAndroid: Boolean
  get() = this is PlatformType.Android

/**
 * Returns true when this platform is iOS.
 */
val PlatformType.isIos: Boolean
  get() = this is PlatformType.Ios
