package com.airsaid.toolkit

actual abstract class ToolkitContext

actual class ToolkitInitializer actual constructor(
  actual val context: ToolkitContext?,
)

actual object Toolkit {

  private var clipboardToolkit: ClipboardToolkit? = null
  private var hapticFeedback: HapticFeedback? = null
  private var networkMonitor: NetworkMonitor? = null
  private var keyboardMonitor: KeyboardMonitor? = null
  private var shareToolkit: ShareToolkit? = null
  private var appLifecycleMonitor: AppLifecycleMonitor? = null
  private var fileToolkit: FileToolkit? = null
  private var sensorToolkit: SensorToolkit? = null

  actual fun initialize(initializer: ToolkitInitializer) {
  }

  actual fun clipboard(): ClipboardToolkit {
    return cached(clipboardToolkit, ClipboardToolkitFactory::create) { clipboardToolkit = it }
  }

  actual fun hapticFeedback(): HapticFeedback {
    return cached(hapticFeedback, HapticFeedbackFactory::create) { hapticFeedback = it }
  }

  actual fun networkMonitor(): NetworkMonitor {
    return cached(networkMonitor, NetworkMonitorFactory::create) { networkMonitor = it }
  }

  actual fun keyboardMonitor(): KeyboardMonitor {
    return cached(keyboardMonitor, KeyboardMonitorFactory::create) { keyboardMonitor = it }
  }

  actual fun shareToolkit(): ShareToolkit {
    return cached(shareToolkit, ShareToolkitFactory::create) { shareToolkit = it }
  }

  actual fun appLifecycleMonitor(): AppLifecycleMonitor {
    return cached(appLifecycleMonitor, AppLifecycleMonitorFactory::create) {
      appLifecycleMonitor = it
    }
  }

  actual fun appInfo(): AppInfo {
    return AppInfoProvider.getAppInfo()
  }

  actual fun deviceInfo(): DeviceInfo {
    return DeviceInfoProvider.getDeviceInfo()
  }

  actual fun appNavigator(): AppNavigator {
    return AppNavigator
  }

  actual fun fileToolkit(): FileToolkit {
    return cached(fileToolkit, FileToolkitFactory::create) { fileToolkit = it }
  }

  actual fun sensorToolkit(): SensorToolkit {
    return cached(sensorToolkit, SensorToolkitFactory::create) { sensorToolkit = it }
  }

  actual fun platformType(): PlatformType {
    return Platform.type
  }

  private inline fun <T> cached(
    current: T?,
    create: () -> T,
    assign: (T) -> Unit,
  ): T {
    return current ?: create().also(assign)
  }
}
