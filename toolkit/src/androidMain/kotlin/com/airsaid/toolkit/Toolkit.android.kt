package com.airsaid.toolkit

import android.content.Context

actual typealias ToolkitContext = Context

actual class ToolkitInitializer actual constructor(
  actual val context: ToolkitContext?,
)

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

  actual fun initialize(initializer: ToolkitInitializer) {
    val context = initializer.context
      ?: throw IllegalStateException(
        "ToolkitInitializer.context is required on Android."
      )
    AppInfoProvider.initialize(context)
    ActivityLifecycleRegistry.initialize(context)
    AppLifecycleMonitorFactory.initialize(context)
    ClipboardToolkitFactory.initialize(context)
    DeviceInfoProvider.initialize(context)
    HapticFeedbackFactory.initialize(context)
    NetworkMonitorFactory.initialize(context)
    AppNavigator.initialize(context)
    KeyboardMonitorFactory.initialize(context)
    ShareToolkitFactory.initialize(context)
    FileToolkitFactory.initialize(context)
    SensorToolkitFactory.initialize(context)
    isInitialized = true
  }

  actual fun clipboard(): ClipboardToolkit {
    ensureInitialized()
    return cached(clipboardToolkit, ClipboardToolkitFactory::create) { clipboardToolkit = it }
  }

  actual fun hapticFeedback(): HapticFeedback {
    ensureInitialized()
    return cached(hapticFeedback, HapticFeedbackFactory::create) { hapticFeedback = it }
  }

  actual fun networkMonitor(): NetworkMonitor {
    ensureInitialized()
    return cached(networkMonitor, NetworkMonitorFactory::create) { networkMonitor = it }
  }

  actual fun keyboardMonitor(): KeyboardMonitor {
    ensureInitialized()
    return cached(keyboardMonitor, KeyboardMonitorFactory::create) { keyboardMonitor = it }
  }

  actual fun shareToolkit(): ShareToolkit {
    ensureInitialized()
    return cached(shareToolkit, ShareToolkitFactory::create) { shareToolkit = it }
  }

  actual fun appLifecycleMonitor(): AppLifecycleMonitor {
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

  actual fun appNavigator(): AppNavigator {
    ensureInitialized()
    return AppNavigator
  }

  actual fun fileToolkit(): FileToolkit {
    ensureInitialized()
    return cached(fileToolkit, FileToolkitFactory::create) { fileToolkit = it }
  }

  actual fun sensorToolkit(): SensorToolkit {
    ensureInitialized()
    return cached(sensorToolkit, SensorToolkitFactory::create) { sensorToolkit = it }
  }

  actual fun platformType(): PlatformType {
    return Platform.type
  }

  private fun ensureInitialized() {
    if (!isInitialized) {
      throw IllegalStateException(
        "Toolkit has not been initialized on Android. " +
            "Call Toolkit.initialize(ToolkitInitializer(context)) first."
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
