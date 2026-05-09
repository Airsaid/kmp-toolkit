package com.airsaid.toolkit

/**
 * Platform-specific application context used to initialize [Toolkit].
 *
 * On Android this is an `android.content.Context`. iOS does not require toolkit initialization.
 */
expect abstract class ToolkitContext

/**
 * Single entry point for toolkit platform services.
 *
 * Android apps must call [initialize] once before accessing platform-backed services.
 * iOS apps can use the accessors directly.
 */
expect object Toolkit {

  /**
   * Initializes platform-specific toolkit components.
   *
   * Android uses the first application [context] passed to this function. Repeated calls
   * are ignored. iOS does not need this call.
   */
  fun initialize(context: ToolkitContext)

  /**
   * Returns a shared [ClipboardToolkit] instance.
   */
  fun clipboard(): ClipboardToolkit

  /**
   * Returns a shared [HapticFeedback] instance.
   */
  fun haptics(): HapticFeedback

  /**
   * Returns a shared [NetworkMonitor] instance.
   */
  fun network(): NetworkMonitor

  /**
   * Returns a shared [KeyboardMonitor] instance.
   */
  fun keyboard(): KeyboardMonitor

  /**
   * Returns a shared [ShareToolkit] instance.
   */
  fun share(): ShareToolkit

  /**
   * Returns a shared [AppLifecycleMonitor] instance.
   */
  fun lifecycle(): AppLifecycleMonitor

  /**
   * Returns the current app metadata snapshot.
   */
  fun appInfo(): AppInfo

  /**
   * Returns the current device metadata snapshot.
   */
  fun deviceInfo(): DeviceInfo

  /**
   * Returns the shared app navigation handler.
   */
  fun navigator(): AppNavigator

  /**
   * Returns a shared [FileToolkit] instance.
   */
  fun files(): FileToolkit

  /**
   * Returns a shared [SensorToolkit] instance.
   */
  fun sensors(): SensorToolkit

  /**
   * Returns the current platform.
   */
  val platform: PlatformType
}
