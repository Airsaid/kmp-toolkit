package com.airsaid.toolkit

import android.content.Context

/**
 * Android implementation of [SensorToolkitFactory].
 */
internal actual object SensorToolkitFactory {

  private var applicationContext: Context? = null

  /**
   * Must be initialized with an Android [Context] before creating [SensorToolkit] instances.
   */
  internal fun initialize(context: Context) {
    applicationContext = context.applicationContext
  }

  /**
   * Creates a [SensorToolkit] instance using the previously initialized context.
   */
  actual fun create(): SensorToolkit {
    val context = applicationContext
      ?: throw IllegalStateException(
        "SensorToolkitFactory must be initialized with Context on Android. " +
            "Call Toolkit.initialize(context) first."
      )
    return SensorToolkitImpl(context)
  }
}
