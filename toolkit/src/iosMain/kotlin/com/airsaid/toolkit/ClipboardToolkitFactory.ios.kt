package com.airsaid.toolkit

/**
 * iOS implementation of [ClipboardToolkitFactory].
 */
internal actual object ClipboardToolkitFactory {

  /**
   * Creates a [ClipboardToolkit] instance for iOS.
   */
  actual fun create(): ClipboardToolkit {
    return ClipboardToolkitImpl()
  }
}
