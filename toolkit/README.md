# toolkit

[中文说明](README.zh.md)

`toolkit` is the Kotlin Multiplatform library module for `kmp-toolkit`. It provides a shared API for app and device capabilities that usually require platform-specific code on Android and iOS.

Use this guide when you want concrete examples for each tool. For a shorter project overview, see the repository [README](../README.md).

## Installation

Add the dependency to the module that needs toolkit APIs, usually `commonMain`:

```kotlin
kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation("com.airsaid:toolkit:$version")
    }
  }
}
```

Make sure Maven Central is available:

```kotlin
repositories {
  mavenCentral()
}
```

## Initialization

Android apps must initialize the toolkit before using platform-backed APIs such as app information, device information, clipboard, network monitoring, keyboard monitoring, sharing, and file picking. iOS does not need an initialization call.

```kotlin
class DemoApp : Application() {
  override fun onCreate() {
    super.onCreate()
    Toolkit.initialize(applicationContext)
  }
}
```

## App Lifecycle

Use `Toolkit.lifecycle()` to observe foreground/background state and start type from shared code.

```kotlin
val monitor = Toolkit.lifecycle()
monitor.startMonitoring()

scope.launch {
  monitor.observeAppLifecycle().collect { status ->
    if (status.isInForeground) {
      // Refresh foreground-only work.
    }
  }
}

val status = monitor.getCurrentStatus()
if (status.isFirstLaunch && status.lastStartType == AppStartType.COLD) {
  // Run first-launch setup.
}

monitor.stopMonitoring()
```

Call `startMonitoring()` before collecting if you need active platform callbacks. Stop monitoring when the owner is no longer interested in lifecycle updates.

## App Info

Use `Toolkit.appInfo()` to read runtime-stable app metadata.

```kotlin
val info = Toolkit.appInfo()
println(info.packageName)
println(info.appName)
println(info.versionName)
println(info.buildNumber)
println(info.buildType)
println(info.buildTime)
```

On Android, `packageName` is the application id. On iOS, it is the bundle identifier.

## Device Info

Use `Toolkit.deviceInfo()` to read device, screen, time zone, and locale information.

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

Screen and manufacturer values are platform-derived and should be treated as runtime information, not as a stable device identity.

## Clipboard

Use `Toolkit.clipboard()` to read, write, clear, and observe clipboard contents. Read snapshots use `ClipboardContent`; writes use `ClipboardWriteContent`. Image snapshots are lightweight references, and image bytes are read only when requested.

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

Set `ClipboardWriteOptions(isSensitive = true)` when copying sensitive content on Android to hide system clipboard previews where supported.

Android clipboard and file-backed sharing use a `FileProvider` authority based on the consuming app id: `${applicationId}.toolkit-clipboard`.

## Network

Use `Toolkit.network()` to observe network connectivity and network type changes.

```kotlin
val monitor = Toolkit.network()
monitor.startMonitoring()

scope.launch {
  monitor.observeNetworkStatus().collect { status ->
    if (!status.isConnected) {
      // Show offline UI.
    }
  }
}

val status = monitor.getCurrentNetworkStatus()
println(status.isConnected)
println(status.type)

val isWifi = status.type == NetworkType.WIFI

monitor.stopMonitoring()
```

Android declares `ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE` for this feature.

## Keyboard

Use `Toolkit.keyboard()` to observe soft keyboard visibility and height changes.

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

Keyboard height is reported in platform pixels and may be `0` when the keyboard is hidden.

## Haptics

Use `Toolkit.haptics()` to trigger common haptic feedback patterns.

```kotlin
val haptics = Toolkit.haptics()

if (haptics.isSupported()) {
  haptics.perform(HapticFeedbackType.SELECTION)
}

haptics.perform(HapticFeedbackType.SUCCESS)
haptics.perform(HapticFeedbackType.WARNING)
haptics.perform(HapticFeedbackType.ERROR)
```

`perform(...)` returns `true` when the request was handled. Android declares `VIBRATE` for haptic feedback.

## Sensors

Use `Toolkit.sensors()` to check sensor availability, observe sensor events, and request snapshots.

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

The shared model includes many sensor types. Platform availability varies; check `isAvailable(...)` before observing. iOS focuses on accelerometer, gyroscope, magnetometer, device motion, and proximity support.

## App Navigation

Use `Toolkit.navigator()` to open common system destinations from shared code.

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

`navigateToAppStoreDetails(appId)` expects an Android package name on Android and an App Store app id on iOS. Navigation methods return `false` when the platform cannot handle the request.

## Share

Use `Toolkit.share()` to present the system share sheet for text, URLs, images, and files.

```kotlin
val shareToolkit = Toolkit.share()

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

`ShareOptions.title` is used as the Android chooser title. `excludedActivities` maps to iOS share sheet exclusions.

## File Picking and Saving

Use `Toolkit.files()` to open platform file pickers, directory pickers, and save destinations. Android uses the Storage Access Framework, and iOS uses `UIDocumentPicker`.

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

`PlatformFile` exposes common metadata and scoped access helpers:

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

Use `withScopedAccess { ... }` for iOS security-scoped file access when you need to work with the selected file beyond simple metadata reads.

## Platform

Use `Toolkit.currentPlatform()` for small platform branches that must stay in common code.

```kotlin
val platform = Toolkit.currentPlatform()

if (platform.isAndroid()) {
  // Android-specific shared-code branch.
}

if (platform.isIos()) {
  // iOS-specific shared-code branch.
}
```

Prefer platform-specific source sets for larger behavior differences.

## Android Permissions

The Android artifact declares the permissions required by toolkit features:

- `ACCESS_NETWORK_STATE`
- `ACCESS_WIFI_STATE`
- `VIBRATE`

Clipboard and share helpers use a `FileProvider` authority based on the consuming app id:
`${applicationId}.toolkit-clipboard`.

## Related Documentation

- Project overview: [../README.md](../README.md)
- Chinese usage guide: [README.zh.md](README.zh.md)
- Changelog: [../CHANGELOG.md](../CHANGELOG.md)
- Release process: [../docs/releasing.md](../docs/releasing.md)
