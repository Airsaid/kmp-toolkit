package com.airsaid.toolkit

/**
 * iOS implementation of [ShareToolkitFactory].
 */
internal actual object ShareToolkitFactory {

  /**
   * Creates a [ShareToolkit] instance for iOS.
   */
  actual fun create(): ShareToolkit {
    return ShareToolkitImpl()
  }
}
