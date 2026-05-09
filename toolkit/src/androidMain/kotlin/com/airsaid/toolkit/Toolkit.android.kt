package com.airsaid.toolkit

import android.content.Context

actual typealias ToolkitContext = Context

actual object Toolkit {

  private var isInitialized: Boolean = false
  private var clipboardToolkit: ClipboardToolkit? = null
  private var hapticFeedback: HapticFeedback? = null
  private var networkMonitor: NetworkMonitor? = null
  private var keyboardMonitor: KeyboardMonitor? = null
  private var shareToolkit: ShareToolkit? = null
  private var appLifecycleMonitor: AppLifecycleMonitor? = null
  private var fileToolkit: FileToolkit? = null
  private var sensorToolkit: SensorToolkit? = null

  actual fun initialize(context: ToolkitContext) {
    if (isInitialized) {
      return
    }
    val applicationContext = context.applicationContext ?: context
    AppInfoProvider.initialize(applicationContext)
    ActivityLifecycleRegistry.initialize(applicationContext)
    AppLifecycleMonitorFactory.initialize(applicationContext)
    ClipboardToolkitFactory.initialize(applicationContext)
    DeviceInfoProvider.initialize(applicationContext)
    HapticFeedbackFactory.initialize(applicationContext)
    NetworkMonitorFactory.initialize(applicationContext)
    AppNavigator.initialize(applicationContext)
    KeyboardMonitorFactory.initialize(applicationContext)
    ShareToolkitFactory.initialize(applicationContext)
    FileToolkitFactory.initialize(applicationContext)
    SensorToolkitFactory.initialize(applicationContext)
    isInitialized = true
  }

  actual fun clipboard(): ClipboardToolkit {
    ensureInitialized()
    return cached(clipboardToolkit, ClipboardToolkitFactory::create) { clipboardToolkit = it }
  }

  actual fun haptics(): HapticFeedback {
    ensureInitialized()
    return cached(hapticFeedback, HapticFeedbackFactory::create) { hapticFeedback = it }
  }

  actual fun network(): NetworkMonitor {
    ensureInitialized()
    return cached(networkMonitor, NetworkMonitorFactory::create) { networkMonitor = it }
  }

  actual fun keyboard(): KeyboardMonitor {
    ensureInitialized()
    return cached(keyboardMonitor, KeyboardMonitorFactory::create) { keyboardMonitor = it }
  }

  actual fun share(): ShareToolkit {
    ensureInitialized()
    return cached(shareToolkit, ShareToolkitFactory::create) { shareToolkit = it }
  }

  actual fun lifecycle(): AppLifecycleMonitor {
    ensureInitialized()
    return cached(appLifecycleMonitor, AppLifecycleMonitorFactory::create) {
      appLifecycleMonitor = it
    }
  }

  actual fun appInfo(): AppInfo {
    ensureInitialized()
    return AppInfoProvider.getAppInfo()
  }

  actual fun deviceInfo(): DeviceInfo {
    ensureInitialized()
    return DeviceInfoProvider.getDeviceInfo()
  }

  actual fun navigator(): AppNavigator {
    ensureInitialized()
    return AppNavigator
  }

  actual fun files(): FileToolkit {
    ensureInitialized()
    return cached(fileToolkit, FileToolkitFactory::create) { fileToolkit = it }
  }

  actual fun sensors(): SensorToolkit {
    ensureInitialized()
    return cached(sensorToolkit, SensorToolkitFactory::create) { sensorToolkit = it }
  }

  actual val platform: PlatformType = PlatformType.Android

  private fun ensureInitialized() {
    if (!isInitialized) {
      throw IllegalStateException(
        "Toolkit has not been initialized on Android. " +
            "Call Toolkit.initialize(context) first."
      )
    }
  }

  private inline fun <T> cached(
    current: T?,
    create: () -> T,
    assign: (T) -> Unit,
  ): T {
    return current ?: create().also(assign)
  }
}
