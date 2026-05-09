package com.airsaid.toolkit

actual abstract class ToolkitContext

actual object Toolkit {

  private var clipboardToolkit: ClipboardToolkit? = null
  private var hapticFeedback: HapticFeedback? = null
  private var networkMonitor: NetworkMonitor? = null
  private var keyboardMonitor: KeyboardMonitor? = null
  private var shareToolkit: ShareToolkit? = null
  private var appLifecycleMonitor: AppLifecycleMonitor? = null
  private var appNavigator: AppNavigator? = null
  private var fileToolkit: FileToolkit? = null
  private var sensorToolkit: SensorToolkit? = null

  actual fun initialize(context: ToolkitContext) {
  }

  actual fun clipboard(): ClipboardToolkit {
    return cached(clipboardToolkit, ClipboardToolkitFactory::create) { clipboardToolkit = it }
  }

  actual fun haptics(): HapticFeedback {
    return cached(hapticFeedback, HapticFeedbackFactory::create) { hapticFeedback = it }
  }

  actual fun network(): NetworkMonitor {
    return cached(networkMonitor, NetworkMonitorFactory::create) { networkMonitor = it }
  }

  actual fun keyboard(): KeyboardMonitor {
    return cached(keyboardMonitor, KeyboardMonitorFactory::create) { keyboardMonitor = it }
  }

  actual fun share(): ShareToolkit {
    return cached(shareToolkit, ShareToolkitFactory::create) { shareToolkit = it }
  }

  actual fun lifecycle(): AppLifecycleMonitor {
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

  actual fun navigator(): AppNavigator {
    return cached(appNavigator, AppNavigatorFactory::create) { appNavigator = it }
  }

  actual fun files(): FileToolkit {
    return cached(fileToolkit, FileToolkitFactory::create) { fileToolkit = it }
  }

  actual fun sensors(): SensorToolkit {
    return cached(sensorToolkit, SensorToolkitFactory::create) { sensorToolkit = it }
  }

  actual val platform: PlatformType = PlatformType.Ios

  private inline fun <T> cached(
    current: T?,
    create: () -> T,
    assign: (T) -> Unit,
  ): T {
    return current ?: create().also(assign)
  }
}
