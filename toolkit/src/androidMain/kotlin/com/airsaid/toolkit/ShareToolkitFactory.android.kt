package com.airsaid.toolkit

import android.content.Context

/**
 * Android implementation of [ShareToolkitFactory].
 */
internal actual object ShareToolkitFactory {

  private var applicationContext: Context? = null

  /**
   * Must be initialized with an Android [Context] before creating [ShareToolkit] instances.
   */
  internal fun initialize(context: Context) {
    applicationContext = context.applicationContext
  }

  /**
   * Creates a [ShareToolkit] instance using the previously initialized context.
   */
  actual fun create(): ShareToolkit {
    val context = applicationContext
      ?: throw IllegalStateException(
        "ShareToolkitFactory must be initialized with Context on Android. " +
            "Call Toolkit.initialize(context) first."
      )
    return ShareToolkitImpl(context)
  }
}
