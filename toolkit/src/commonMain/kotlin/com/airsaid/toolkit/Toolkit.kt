package com.airsaid.toolkit

/**
 * Platform-specific context type used by [ToolkitInitializer].
 */
expect abstract class ToolkitContext

/**
 * Initializes toolkit dependencies for each platform.
 */
expect class ToolkitInitializer {

  val context: ToolkitContext?

  constructor(
    context: ToolkitContext? = null,
  )
}

/**
 * Single entry point for initializing the toolkit module.
 */
expect object Toolkit {

  /**
   * Initializes platform-specific toolkit components.
   *
   * Android requires initialization before use. iOS does not.
   */
  fun initialize(initializer: ToolkitInitializer)

  /**
   * Returns a shared [ClipboardToolkit] instance.
   */
  fun clipboard(): ClipboardToolkit

  /**
   * Returns a shared [HapticFeedback] instance.
   */
  fun hapticFeedback(): HapticFeedback

  /**
   * Returns a shared [NetworkMonitor] instance.
   */
  fun networkMonitor(): NetworkMonitor

  /**
   * Returns a shared [KeyboardMonitor] instance.
   */
  fun keyboardMonitor(): KeyboardMonitor

  /**
   * Returns a shared [ShareToolkit] instance.
   */
  fun shareToolkit(): ShareToolkit

  /**
   * Returns a shared [AppLifecycleMonitor] instance.
   */
  fun appLifecycleMonitor(): AppLifecycleMonitor

  /**
   * Returns current [AppInfo].
   */
  fun appInfo(): AppInfo

  /**
   * Returns current [DeviceInfo].
   */
  fun deviceInfo(): DeviceInfo

  /**
   * Returns app navigation handler.
   */
  fun appNavigator(): AppNavigator

  /**
   * Returns a shared [FileToolkit] instance.
   */
  fun fileToolkit(): FileToolkit

  /**
   * Returns a shared [SensorToolkit] instance.
   */
  fun sensorToolkit(): SensorToolkit

  /**
   * Returns current platform type.
   */
  fun platformType(): PlatformType
}
