# toolkit

[English](README.md)

`toolkit` 是 `kmp-toolkit` 的 Kotlin Multiplatform 库模块，为 Android 与 iOS 上常见但通常需要平台代码的 App 与设备能力提供统一的共享 API。

如果你需要查看每个工具的具体调用示例，请使用本文档。更简短的项目概览见仓库根目录 [README](../README.zh.md)。

## 安装

在需要使用 toolkit API 的模块中添加依赖，通常是 `commonMain`：

```kotlin
kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation("com.airsaid:toolkit:$version")
    }
  }
}
```

确保已添加 Maven Central：

```kotlin
repositories {
  mavenCentral()
}
```

## 初始化

Android 端在使用 App 信息、设备信息、剪贴板、网络监听、键盘监听、系统分享、文件选择等平台能力前，必须先初始化 toolkit。iOS 端无需初始化。

```kotlin
class DemoApp : Application() {
  override fun onCreate() {
    super.onCreate()
    Toolkit.initialize(
      ToolkitInitializer(applicationContext)
    )
  }
}
```

## 应用生命周期

使用 `Toolkit.appLifecycleMonitor()` 在共享代码中监听前后台状态与启动类型。

```kotlin
val monitor = Toolkit.appLifecycleMonitor()
monitor.startMonitoring()

scope.launch {
  monitor.observeAppLifecycle().collect { status ->
    if (status.isInForeground) {
      // 执行仅前台需要刷新的任务。
    }
  }
}

val status = monitor.getCurrentStatus()
if (status.isFirstLaunch && status.lastStartType == AppStartType.COLD) {
  // 执行首次启动初始化。
}

monitor.stopMonitoring()
```

如果需要接收平台回调，请先调用 `startMonitoring()` 再订阅。调用方不再需要生命周期更新时应停止监听。

## 应用信息

使用 `Toolkit.appInfo()` 读取运行期间稳定的应用元数据。

```kotlin
val info = Toolkit.appInfo()
println(info.packageName)
println(info.appName)
println(info.versionName)
println(info.buildNumber)
println(info.buildType)
println(info.buildTime)
```

Android 上 `packageName` 是 application id；iOS 上是 bundle identifier。

## 设备信息

使用 `Toolkit.deviceInfo()` 读取设备、屏幕、时区与语言偏好信息。

```kotlin
val device = Toolkit.deviceInfo()
println(device.deviceModel)
println(device.systemName)
println(device.systemVersion)
println(device.systemVersionCode)
println(device.manufacturer.manufacturer)
println(device.manufacturer.brand)
println(device.deviceType.isTablet)
println(device.deviceType.isEmulator)
println(device.screen.widthPx)
println(device.screen.heightPx)
println(device.screen.density)
println(device.timeZone.id)
println(device.timeZone.offsetMinutes)
println(device.locale.current.languageCode)
println(device.locale.current.regionCode)
println(device.locale.current.tag)
println(device.locale.preferred)
```

屏幕与厂商信息来自平台运行时，应视为运行环境信息，不应当作稳定设备身份。

## 剪贴板

使用 `Toolkit.clipboard()` 读取、写入、清空并监听剪贴板内容。文本、富文本、URI 与图片内容由 `ClipboardContent` 表示。

```kotlin
val clipboard = Toolkit.clipboard()

clipboard.setText("hello")

clipboard.setContents(
  listOf(
    ClipboardContent.RichText(
      text = "<p><b>Hello</b></p>",
      format = RichTextFormat.HTML,
      plainText = "Hello",
    ),
    ClipboardContent.Uri("https://example.com"),
  )
)

val snapshot = clipboard.getSnapshot()
val text = clipboard.getText()
val hasText = clipboard.hasText()

scope.launch {
  clipboard.observeClipboard().collect { latest ->
    println(latest.contents)
  }
}

clipboard.clear()
```

Android 剪贴板与文件类分享会使用基于接入方应用 id 的 `FileProvider` authority：`${applicationId}.toolkit-clipboard`。

## 网络

使用 `Toolkit.networkMonitor()` 监听网络连接状态与网络类型变化。

```kotlin
val monitor = Toolkit.networkMonitor()
monitor.startMonitoring()

scope.launch {
  monitor.observeNetworkStatus().collect { status ->
    if (!status.isConnected) {
      // 展示离线状态 UI。
    }
  }
}

val status = monitor.getCurrentNetworkStatus()
println(status.isConnected)
println(status.type)

val isWifi = status.type == NetworkType.WIFI

monitor.stopMonitoring()
```

Android 会为该能力声明 `ACCESS_NETWORK_STATE` 与 `ACCESS_WIFI_STATE`。

## 键盘

使用 `Toolkit.keyboardMonitor()` 监听软键盘可见性与高度变化。

```kotlin
val keyboardMonitor = Toolkit.keyboardMonitor()
keyboardMonitor.startMonitoring()

scope.launch {
  keyboardMonitor.observeKeyboardStatus().collect { status ->
    if (status.isVisible) {
      println(status.heightPx)
    }
  }
}

val status = keyboardMonitor.getCurrentStatus()
println(status.isVisible)
println(status.heightPx)

keyboardMonitor.stopMonitoring()
```

键盘高度使用平台像素表示，键盘隐藏时可能为 `0`。

## 触感反馈

使用 `Toolkit.hapticFeedback()` 触发常见触感反馈。

```kotlin
val haptics = Toolkit.hapticFeedback()

if (haptics.isSupported()) {
  haptics.perform(HapticFeedbackType.SELECTION)
}

haptics.perform(HapticFeedbackType.SUCCESS)
haptics.perform(HapticFeedbackType.WARNING)
haptics.perform(HapticFeedbackType.ERROR)
```

`perform(...)` 在请求被处理时返回 `true`。Android 会为触感反馈声明 `VIBRATE`。

## 传感器

使用 `Toolkit.sensorToolkit()` 检查传感器可用性、监听传感器事件并获取快照。

```kotlin
val sensors = Toolkit.sensorToolkit()
val availability = sensors.isAvailable(SensorType.ACCELEROMETER)

if (availability.isAvailable) {
  scope.launch {
    sensors.observe(
      type = SensorType.ACCELEROMETER,
      options = SensorOptions(
        samplingRateHz = 50,
      ),
    ).collect { event ->
      val x = event.values[0]
      val y = event.values[1]
      val z = event.values[2]
    }
  }
}

val snapshot = sensors.getSnapshot(SensorType.GYROSCOPE)
val gyroValues = snapshot?.values

sensors.stop(SensorType.ACCELEROMETER)
```

共享模型包含多种传感器类型。平台可用性不同，监听前请先调用 `isAvailable(...)`。iOS 侧优先覆盖加速度计、陀螺仪、磁力计、设备姿态与距离传感器。

## 应用级跳转

使用 `Toolkit.appNavigator()` 在共享代码中打开常见系统目标页。

```kotlin
val navigator = Toolkit.appNavigator()

val openedSystem = navigator.navigateToSystemSettings()
val openedApp = navigator.navigateToAppDetails()
val openedNotifications = navigator.navigateToNotificationSettings()
val openedUrl = navigator.navigateToUrl("https://example.com")
val openedEmail = navigator.navigateToEmail(
  to = "support@example.com",
  subject = "Feedback",
  body = "Describe the issue",
)
val openedDial = navigator.navigateToDial("10086")
val openedSms = navigator.navigateToSms(
  phone = "10086",
  body = "Hello",
)
val openedStore = navigator.navigateToAppStoreDetails("com.example.app")
```

`navigateToAppStoreDetails(appId)` 在 Android 侧传包名，在 iOS 侧传 App Store 应用 ID。当平台无法处理请求时，跳转方法会返回 `false`。

## 系统分享

使用 `Toolkit.shareToolkit()` 调起系统分享面板，支持文本、链接、图片与文件。

```kotlin
val shareToolkit = Toolkit.shareToolkit()

shareToolkit.shareText(
  text = "hello",
  options = ShareOptions(title = "Share"),
)

shareToolkit.shareUrl(
  url = "https://example.com",
  options = ShareOptions(title = "Share"),
)

shareToolkit.share(
  contents = listOf(
    ShareContent.Text("hello"),
    ShareContent.Url("https://example.com"),
  ),
  options = ShareOptions(
    title = "Share",
    excludedActivities = listOf(
      ShareExcludedActivity.COPY_TO_PASTEBOARD,
    ),
  ),
)
```

`ShareOptions.title` 会作为 Android chooser 标题使用。`excludedActivities` 会映射到 iOS 分享面板排除项。

## 文件选择与保存

使用 `Toolkit.fileToolkit()` 打开系统文件选择器、目录选择器与保存目标。Android 使用 Storage Access Framework，iOS 使用 `UIDocumentPicker`。

```kotlin
val files = Toolkit.fileToolkit()

val picked = files.pickFile(
  FilePickerOptions(
    title = "Choose a file",
    type = PlatformFileType.File(listOf("pdf", "docx")),
  )
)

val selected = files.pickFiles(
  FilePickerOptions(
    title = "Choose images",
    type = PlatformFileType.Image,
    mode = FilePickerMode.Multiple(maxItems = 5),
  )
)

val directory = files.pickDirectory(
  DirectoryPickerOptions(title = "Choose a folder")
)

val saved = files.saveFile(
  FileSaveOptions(
    suggestedName = "document",
    extension = "txt",
  )
)
```

`PlatformFile` 暴露通用元数据与作用域访问辅助方法：

```kotlin
val file = files.pickFile()
if (file != null) {
  val name = file.name
  val extension = file.extension
  val path = file.path
  val size = file.size()
  val mimeType = file.mimeType()
  val exists = file.exists()
}
```

当 iOS 端需要在简单元数据读取之外继续访问所选文件时，使用 `withScopedAccess { ... }` 处理安全作用域访问。

## 平台判断

使用 `Toolkit.platformType()` 处理必须保留在 common 代码中的小型平台分支。

```kotlin
when (Toolkit.platformType()) {
  PlatformType.ANDROID -> {
    // Android 侧共享代码分支。
  }
  PlatformType.IOS -> {
    // iOS 侧共享代码分支。
  }
}
```

如果平台差异较大，优先使用平台 source set。

## Android 权限

Android 产物声明了 toolkit 功能所需权限：

- `ACCESS_NETWORK_STATE`
- `ACCESS_WIFI_STATE`
- `VIBRATE`

剪贴板与分享能力使用基于接入方应用 id 的 `FileProvider` authority：
`${applicationId}.toolkit-clipboard`。

## 相关文档

- 项目概览：[../README.zh.md](../README.zh.md)
- 英文使用文档：[README.md](README.md)
- 变更记录：[../CHANGELOG.md](../CHANGELOG.md)
- 发布流程：[../docs/releasing.md](../docs/releasing.md)
