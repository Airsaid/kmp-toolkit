# toolkit

toolkit 是一组支持跨平台的系统能力封装，涵盖设备与应用信息、剪贴板、网络状态、键盘可见性、系统分享与设置页跳转等能力，便于在多端共享统一的调用方式。

## 使用方式

在需要使用的模块中添加依赖：

```kotlin
dependencies {
      implementation("com.airsaid:toolkit:$version")
}
```

## 初始化

Android 端需要先初始化后才能使用（包含 App 信息、设备信息、剪贴板、网络状态、键盘监听、系统分享等），且统一通过 `Toolkit.initialize(...)` 完成；iOS 端无需调用：

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

## 能力与调用示例

### 应用生命周期监听

提供跨平台的应用生命周期状态变化监听能力。

```kotlin
val monitor = Toolkit.appLifecycleMonitor()
monitor.startMonitoring() // 启动监听
scope.launch {
  monitor.observeAppLifecycle().collect { status -> // 订阅生命周期变化
    if (status.isInForeground) { // 判断应用是否在前台

    }
  }
}
val status = monitor.getCurrentStatus() // 获取当前状态
if (status.isFirstLaunch && status.lastStartType == AppStartType.COLD) { // 判断是否是首次冷启动

}
monitor.stopMonitoring() // 停止监听
```

### 应用信息获取

提供应用稳定元数据（包名、App 名称、版本名、构建号、buildType、buildTime）获取能力。

```kotlin
val info = Toolkit.appInfo()
println(info.packageName) // 获取包名
println(info.appName) // 获取 App 名称
println(info.versionName) // 获取版本名
println(info.buildNumber) // 获取构建号
println(info.buildType) // 获取 buildType
println(info.buildTime) // 获取 buildTime
```

### 剪贴板读写与监听

提供跨平台剪贴板的文本、富文本、图片与 URI 读写与变化监听能力。

```kotlin
val clipboard = Toolkit.clipboard()
clipboard.setText("hello") // 写入纯文本
clipboard.setContents( // 写入多类型内容
  listOf(
    ClipboardContent.RichText(
      text = "<p><b>Hello</b></p>",
      format = RichTextFormat.HTML,
      plainText = "Hello",
    ),
    ClipboardContent.Uri("https://example.com"),
  )
)
val snapshot = clipboard.getSnapshot() // 读取当前剪贴板内容
val text = clipboard.getText() // 读取第一个文本内容
scope.launch {
  clipboard.observeClipboard().collect { latest -> // 监听剪贴板内容变化

  }
}
clipboard.clear() // 清空剪贴板
```

### 触感反馈

提供触感反馈能力，并可判断设备是否支持。

```kotlin
val haptics = Toolkit.hapticFeedback()
if (haptics.isSupported()) { // 判断是否支持触感反馈
  haptics.perform(HapticFeedbackType.SELECTION) // 触发选择反馈
}
haptics.perform(HapticFeedbackType.SUCCESS) // 触发成功反馈
haptics.perform(HapticFeedbackType.WARNING) // 触发警告反馈
haptics.perform(HapticFeedbackType.ERROR) // 触发错误反馈
```

### 网络状态监听

提供网络连接状态与类型变化监听能力。

```kotlin
val monitor = Toolkit.networkMonitor()
monitor.startMonitoring() // 启动监听
scope.launch {
  monitor.observeNetworkStatus().collect { status -> // 订阅网络状态变化
    if (!status.isConnected) {

    }
  }
}
val status = monitor.getCurrentNetworkStatus() // 获取当前网络状态
println(status.isConnected) // 是否已连接
println(status.type) // 当前网络类型
val isWifi = status.type == NetworkType.WIFI // 判断是否 Wi-Fi
monitor.stopMonitoring() // 停止监听
```

### 传感器能力

提供跨平台传感器读取能力，当前优先覆盖加速度计、陀螺仪、磁力计与设备姿态（device motion）。iOS 端暂不支持气压计与计步类传感器。

```kotlin
val sensors = Toolkit.sensorToolkit()
if (sensors.isAvailable(SensorType.ACCELEROMETER).isAvailable) {
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
val gyro = snapshot?.values
```

### 应用级跳转

提供跳转系统设置页、应用详情页、通知设置、打开链接、邮件/拨号/短信与应用商店详情页的能力。

```kotlin
val navigator = Toolkit.appNavigator()
val openedSystem = navigator.navigateToSystemSettings() // 打开系统设置
val openedApp = navigator.navigateToAppDetails() // 打开应用详情
val openedNotifications = navigator.navigateToNotificationSettings() // 打开通知设置
val openedEmail = navigator.navigateToEmail( // 打开邮件
  to = "support@example.com",
  subject = "反馈",
  body = "请描述你的问题",
)
val openedDial = navigator.navigateToDial("10086") // 打开拨号
val openedSms = navigator.navigateToSms( // 打开短信
  phone = "10086",
  body = "你好",
)
val openedStore = navigator.navigateToAppStoreDetails("com.example.app") // 打开应用商店详情
val openedUrl = navigator.navigateToUrl("https://example.com") // 打开链接
```

说明：`navigateToAppStoreDetails(appId)` 在 Android 侧传包名，在 iOS 侧传 App Store 的应用 ID。

### 系统分享

提供调用系统分享面板的能力，支持文本、链接、图片与文件。

```kotlin
val shareToolkit = Toolkit.shareToolkit()
shareToolkit.shareText(
  text = "hello",
  options = ShareOptions(title = "分享到"),
)
shareToolkit.shareUrl(
  url = "https://example.com",
  options = ShareOptions(title = "分享到"),
)
shareToolkit.share(
  contents = listOf(
    ShareContent.Text("hello"),
    ShareContent.Url("https://example.com"),
  ),
  options = ShareOptions(
    title = "分享到",
    excludedActivities = listOf(
      ShareExcludedActivity.COPY_TO_PASTEBOARD,
    ),
  ),
)
```

### 文件选择与保存

提供系统文件选择与保存能力（Android 使用 SAF，iOS 使用 UIDocumentPicker）。

```kotlin
val files = Toolkit.fileToolkit()
val picked = files.pickFile(
  FilePickerOptions(
    title = "选择文件",
    type = PlatformFileType.File(listOf("pdf", "docx")),
  )
)
val selected = files.pickFiles(
  FilePickerOptions(
    title = "选择图片",
    type = PlatformFileType.Image,
    mode = FilePickerMode.Multiple(maxItems = 5),
  )
)
val directory = files.pickDirectory(
  DirectoryPickerOptions(title = "选择文件夹")
)
val saved = files.saveFile(
  FileSaveOptions(
    suggestedName = "document",
    extension = "txt",
  )
)
```

#### PlatformFile 元信息示例

```kotlin
val file = files.pickFile()
if (file != null) {
  val name = file.name
  val size = file.size()
  val mimeType = file.mimeType()
  val exists = file.exists()
}
```

### 设备信息获取

提供设备信息（设备型号、系统信息、屏幕信息、时区与语言偏好）获取能力。

```kotlin
val device = Toolkit.deviceInfo()
println(device.deviceModel) // 设备型号
println(device.systemName) // 系统名称
println(device.systemVersion) // 系统版本
println(device.systemVersionCode) // 系统版本号
println(device.manufacturer.manufacturer) // 厂商
println(device.manufacturer.brand) // 品牌
println(device.deviceType.isTablet) // 是否平板
println(device.deviceType.isEmulator) // 是否模拟器
println(device.screen.widthPx) // 屏幕宽度 px
println(device.screen.heightPx) // 屏幕高度 px
println(device.screen.density) // 屏幕密度
println(device.timeZone.id) // 时区 ID
println(device.timeZone.offsetMinutes) // 时区偏移(分钟)
println(device.locale.current.languageCode) // 当前语言代码
println(device.locale.current.regionCode) // 当前地区代码
println(device.locale.current.tag) // 当前语言标签
println(device.locale.preferred) // 首选语言列表
```

### 平台判断

提供统一的平台类型判断能力，便于在 common 代码中进行分支处理。

```kotlin
when (Toolkit.platformType()) {
  PlatformType.ANDROID -> {
    
  }
  PlatformType.IOS -> {
    
  }
}
```

### 键盘状态监听

提供软键盘可见性与高度变化监听能力。

```kotlin
val keyboardMonitor = Toolkit.keyboardMonitor()
keyboardMonitor.startMonitoring() // 启动监听
scope.launch {
  keyboardMonitor.observeKeyboardStatus().collect { status -> // 订阅键盘状态变化
    if (status.isVisible) {

    }
  }
}
val status = keyboardMonitor.getCurrentStatus() // 获取当前键盘状态
println(status.isVisible) // 是否可见
println(status.heightPx) // 键盘高度
keyboardMonitor.stopMonitoring() // 停止监听
```
