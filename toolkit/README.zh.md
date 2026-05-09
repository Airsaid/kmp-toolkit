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
    Toolkit.initialize(applicationContext)
  }
}
```

## 应用生命周期

使用 `Toolkit.lifecycle()` 在共享代码中监听前后台状态与启动事件。

```kotlin
val monitor = Toolkit.lifecycle()

scope.launch {
  monitor.observeAppLifecycle().collect { status ->
    if (status.isInForeground) {
      // 执行仅前台需要刷新的任务。
    }
  }
}

scope.launch {
  monitor.observeAppStartEvents().collect { startType ->
    if (startType == AppStartType.COLD) {
      // 执行冷启动初始化。
    }
  }
}

val status = monitor.getCurrentStatus()
println(status.lastStartType)
```

订阅 `observeAppLifecycle()` 或 `observeAppStartEvents()` 时会自动开始监听；只需要快照时可直接调用 `getCurrentStatus()`。

## 应用信息

使用 `Toolkit.appInfo()` 读取运行期间稳定的应用元数据。

```kotlin
val info = Toolkit.appInfo()
println(info.packageName)
println(info.appName)
println(info.versionName)
println(info.buildNumber)
```

Android 上 `packageName` 是 application id；iOS 上是 bundle identifier。
当平台元数据不可用时，`appName`、`versionName` 与 `buildNumber` 会返回 `null`。

## 设备信息

使用 `Toolkit.deviceInfo()` 读取设备、窗口、屏幕、时区与语言偏好信息。

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
println(device.window?.widthPx)
println(device.window?.heightPx)
println(device.window?.widthLogical)
println(device.window?.heightLogical)
println(device.screen.widthPx)
println(device.screen.heightPx)
println(device.screen.widthLogical)
println(device.screen.heightLogical)
println(device.screen.density)
println(device.timeZone.id)
println(device.timeZone.offsetMinutes)
println(device.locale.current.languageTag)
println(device.locale.current.languageCode)
println(device.locale.current.scriptCode)
println(device.locale.current.regionCode)
println(device.locale.preferred)
```

`window` 表示当前前台应用/窗口视口（可获取时），`screen` 表示平台主屏幕快照。`widthLogical` 与 `heightLogical` 在 Android 上是 dp，在 iOS 上是 points。设备类型与模拟器字段都是启发式判断；不要用于布局断点、安全、授权或反作弊校验。

## 剪贴板

使用 `Toolkit.clipboard()` 读取、写入、清空并监听剪贴板内容。读取快照使用 `ClipboardContent`，写入使用 `ClipboardWriteContent`。图片快照是轻量引用，只有显式请求时才读取图片字节。

```kotlin
val clipboard = Toolkit.clipboard()

scope.launch {
  clipboard.setText("hello")

  clipboard.setContents(
    listOf(
      ClipboardWriteContent.RichText(
        content = "<p><b>Hello</b></p>",
        format = RichTextFormat.HTML,
        plainText = "Hello",
      ),
      ClipboardWriteContent.Uri("https://example.com"),
    )
  )

  val snapshot = clipboard.getSnapshot()
  val text = clipboard.getText()
  val hasText = clipboard.hasText()
  val imageBytes = snapshot.contents
    .filterIsInstance<ClipboardContent.Image>()
    .firstOrNull()
    ?.let { clipboard.readImageBytes(it) }

  clipboard.clear()
}

scope.launch {
  clipboard.observeClipboard().collect { latest ->
    println(latest.contents)
  }
}
```

在 Android 上复制敏感内容时，可传入 `ClipboardWriteOptions(isSensitive = true)` 以在支持的平台隐藏系统剪贴板预览。

Android 剪贴板与文件类分享会使用基于接入方应用 id 的 `FileProvider` authority：`${applicationId}.toolkit-clipboard`。

## 网络

使用 `Toolkit.network()` 监听默认网络连接状态与传输类型变化。

```kotlin
val monitor = Toolkit.network()

scope.launch {
  monitor.observeNetworkStatus().collect { status ->
    if (!status.isConnected) {
      // 展示离线状态 UI。
    }
  }
}

val status = monitor.getCurrentNetworkStatus()
println(status.isConnected)
println(status.transports)

val isWifi = NetworkTransport.WIFI in status.transports
```

Android 会为该能力声明 `ACCESS_NETWORK_STATE`。

## 键盘

使用 `Toolkit.keyboard()` 监听软键盘可见性与高度变化。

```kotlin
val keyboardMonitor = Toolkit.keyboard()
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

使用 `Toolkit.haptics()` 触发跨平台语义触感反馈，尤其适合非 Compose 代码，或希望在共享逻辑中
表达成功、警告、错误、选择等语义反馈且不依赖具体 UI 框架的场景。

```kotlin
val haptics = Toolkit.haptics()

haptics.perform(HapticFeedbackType.SELECTION)
haptics.perform(HapticFeedbackType.SUCCESS)
haptics.perform(HapticFeedbackType.WARNING)
haptics.perform(HapticFeedbackType.ERROR)
```

`perform(...)` 在平台接受请求时返回 `true`，但不保证用户一定感受到触感反馈。

对于 Android UI 交互，优先使用基于 View 的扩展。它使用
`View.performHapticFeedback(...)`，会遵循系统和 View 的触感反馈设置，并且不需要
`VIBRATE`。

```kotlin
view.performHapticFeedback(HapticFeedbackType.SELECTION)
```

对于 Compose UI 交互，优先使用 Compose 的 `LocalHapticFeedback`。它作用于当前 composition，
更适合长按、拖拽、文本手柄移动等直接 UI 手势。`Toolkit.haptics()` 仍适合 Compose UI 之外，
或需要 toolkit 层跨平台语义反馈的场景。

## 传感器

使用 `Toolkit.sensors()` 检查传感器可用性、监听传感器事件并获取快照。

```kotlin
val sensors = Toolkit.sensors()
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

使用 `Toolkit.navigator()` 在共享代码中打开常见系统目标页。

```kotlin
val navigator = Toolkit.navigator()

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

使用 `Toolkit.share()` 调起系统分享面板，支持文本、链接、图片与文件。分享方法为
suspend API，并返回 `ShareResult`。

```kotlin
val shareToolkit = Toolkit.share()

val textResult = shareToolkit.shareText(
  text = "hello",
  options = ShareOptions(title = "Share"),
)

val urlResult = shareToolkit.shareUrl(
  url = "https://example.com",
  options = ShareOptions(title = "Share"),
)

val result = shareToolkit.share(
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

`ShareResult.Completed` 表示平台确认完成或用户选择了分享目标。Android 侧仅表示用户选择了目标应用，不保证接收方一定完成发送或保存。`ShareResult.Cancelled` 表示平台确认用户取消，`ShareResult.Failed` 表示分享面板未能成功调起。

`ShareOptions.title` 会作为 Android chooser 标题使用。`excludedActivities` 会映射到 iOS 分享面板排除项。现有字符串路径和 URI 仍可通过 `ShareContent.File` 分享；`FileToolkit` 返回的文件可通过 `ShareContent.PlatformFile` 或 `shareFile(file = platformFile)` 分享。

## 文件选择与保存

使用 `Toolkit.files()` 打开系统文件选择器、目录选择器与保存目标。Android 使用 Storage Access Framework，iOS 使用 `UIDocumentPicker`。

```kotlin
val files = Toolkit.files()

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

使用 `Toolkit.currentPlatform()` 处理必须保留在 common 代码中的小型平台分支。

```kotlin
val platform = Toolkit.currentPlatform()

if (platform.isAndroid()) {
  // Android 侧共享代码分支。
}

if (platform.isIos()) {
  // iOS 侧共享代码分支。
}
```

如果平台差异较大，优先使用平台 source set。

## Android 权限

Android 产物声明了 toolkit 功能所需权限：

- `ACCESS_NETWORK_STATE`

剪贴板与分享能力使用基于接入方应用 id 的 `FileProvider` authority：
`${applicationId}.toolkit-clipboard`。

## 相关文档

- 项目概览：[../README.zh.md](../README.zh.md)
- 英文使用文档：[README.md](README.md)
- 变更记录：[../CHANGELOG.md](../CHANGELOG.md)
- 发布流程：[../docs/releasing.md](../docs/releasing.md)
