package com.airsaid.toolkit

import android.content.Context

/**
 * Android implementation of [ClipboardToolkitFactory].
 */
internal actual object ClipboardToolkitFactory {

  private var applicationContext: Context? = null

  /**
   * Must be initialized with an Android [Context] before creating [ClipboardToolkit] instances.
   */
  internal fun initialize(context: Context) {
    applicationContext = context.applicationContext
  }

  /**
   * Creates a [ClipboardToolkit] instance using the previously initialized context.
   */
  actual fun create(): ClipboardToolkit {
    val context = applicationContext
      ?: throw IllegalStateException(
        "ClipboardToolkitFactory must be initialized with Context on Android. " +
            "Call Toolkit.initialize(ToolkitInitializer(context)) first."
      )
    return ClipboardToolkitImpl(context)
  }
}
