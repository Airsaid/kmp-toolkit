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

Use `Toolkit.lifecycle()` to observe foreground/background state and launch events from shared code.

```kotlin
val monitor = Toolkit.lifecycle()

scope.launch {
  monitor.observeAppLifecycle().collect { status ->
    if (status.isInForeground) {
      // Refresh foreground-only work.
    }
  }
}

scope.launch {
  monitor.observeAppStartEvents().collect { startType ->
    if (startType == AppStartType.COLD) {
      // Run cold-start setup.
    }
  }
}

val status = monitor.getCurrentStatus()
println(status.lastStartType)
```

Lifecycle monitoring starts automatically while `observeAppLifecycle()` or `observeAppStartEvents()` is collected. Use `getCurrentStatus()` when you only need a snapshot.

## App Info

Use `Toolkit.appInfo()` to read runtime-stable app metadata.

```kotlin
val info = Toolkit.appInfo()
println(info.packageName)
println(info.appName)
println(info.versionName)
println(info.buildNumber)
```

On Android, `packageName` is the application id. On iOS, it is the bundle identifier.
`appName`, `versionName`, and `buildNumber` are nullable when the platform metadata is unavailable.

## Device Info

Use `Toolkit.deviceInfo()` to read device, window, screen, time zone, and locale information.

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

`window` is the current foreground app/window viewport when available. `screen` is a platform main screen snapshot. `widthLogical` and `heightLogical` are Android dp and iOS points. Device type and emulator flags are heuristics; do not use them for layout breakpoints, security, licensing, or anti-abuse checks.

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

Use `Toolkit.network()` to observe default network connectivity and transport changes.

```kotlin
val monitor = Toolkit.network()

scope.launch {
  monitor.observeNetworkStatus().collect { status ->
    if (!status.isConnected) {
      // Show offline UI.
    }
  }
}

val status = monitor.getCurrentNetworkStatus()
println(status.isConnected)
println(status.transports)

val isWifi = NetworkTransport.WIFI in status.transports
```

Android declares `ACCESS_NETWORK_STATE` for this feature.

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

Use `Toolkit.haptics()` to trigger cross-platform semantic haptic feedback, especially from
non-Compose code or shared logic that wants success, warning, error, or selection feedback without
depending on a UI framework.

```kotlin
val haptics = Toolkit.haptics()

haptics.perform(HapticFeedbackType.SELECTION)
haptics.perform(HapticFeedbackType.SUCCESS)
haptics.perform(HapticFeedbackType.WARNING)
haptics.perform(HapticFeedbackType.ERROR)
```

`perform(...)` returns `true` when the request was accepted by the platform. This does not guarantee
that the user felt haptic feedback.

For Android UI interactions, prefer the View-based extension because it uses
`View.performHapticFeedback(...)`, honors system and view haptic settings, and does not require
`VIBRATE`.

```kotlin
view.performHapticFeedback(HapticFeedbackType.SELECTION)
```

For Compose UI interactions, prefer Compose's `LocalHapticFeedback`. It is scoped to the current
composition and is the best fit for direct UI gestures such as long press, drag, and text handle
movement. `Toolkit.haptics()` remains useful outside Compose UI or when you want toolkit-level
cross-platform semantic feedback.

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

val openedSystem = navigator.openSystemSettings()
val openedApp = navigator.openAppSettings()
val openedNotifications = navigator.openNotificationSettings()
val openedUrl = navigator.openUrl("https://example.com")
val openedEmail = navigator.openEmail(
  to = "support@example.com",
  subject = "Feedback",
  body = "Describe the issue",
)
val openedDial = navigator.openDial("10086")
val openedSms = navigator.openSms(
  phoneNumber = "10086",
  body = "Hello",
)
val openedStore = navigator.openAppStoreDetails(
  AppStoreDetailsNavigationRequest(
    androidPackageName = "com.example.app",
    iosAppId = "123456789",
  )
)
```

Navigation methods return an `AppNavigationResult`. `Presented(destination)` means the platform accepted the request, while `Failed(reason)` reports invalid input, unsupported destinations, missing target apps, security denial, or presentation failures. iOS does not expose a system-wide settings destination, so `openSystemSettings()` returns `UNSUPPORTED_DESTINATION` there. Notification settings can fall back to app settings on platforms or versions without a dedicated destination. `openAppStoreDetails(...)` uses `androidPackageName` on Android and `iosAppId` on iOS.

## Share

Use `Toolkit.share()` to present the system share sheet for text, URLs, images, and files.
Share calls are suspending and return a `ShareResult`.

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

`ShareResult.Completed` means the platform confirmed completion or target selection. On Android, it means the user selected a share target and does not guarantee the receiving app actually sent or saved the content. `ShareResult.Cancelled` is returned when the platform confirms cancellation. `ShareResult.Failed` describes requests that could not be presented.

`ShareOptions.title` is used as the Android chooser title. `excludedActivities` maps to iOS share sheet exclusions. Existing string file paths and URIs remain supported with `ShareContent.File`, and files returned by `FileToolkit` can be shared with `ShareContent.PlatformFile` or `shareFile(file = platformFile)`.

## File Picking and Saving

Use `Toolkit.files()` to open platform file pickers, directory pickers, and create writable file targets. Android uses the Storage Access Framework, and iOS uses `UIDocumentPicker`.

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
  ),
  maxItems = 5,
)

val directory = files.pickDirectory(
  DirectoryPickerOptions(title = "Choose a folder")
)

val created = files.createFile(
  FileCreateOptions(
    suggestedName = "document",
    extension = "txt",
  )
)
```

`createFile` only creates or selects a writable target. Write content to the returned platform file with platform I/O APIs.

`PlatformFile` exposes common metadata and scoped access helpers:

```kotlin
val file = files.pickFile()
if (file != null) {
  val name = file.name
  val extension = file.extension
  val path = file.path
  val size = file.size() // null when the platform cannot provide a size.
  val mimeType = file.mimeType()
  val exists = file.exists()
}
```

Use `withScopedAccess { ... }` for iOS security-scoped file access when you need to work with the selected file beyond simple metadata reads.

## Platform

Use `Toolkit.platform` for small platform branches that must stay in common code.

```kotlin
val platform = Toolkit.platform

if (platform.isAndroid) {
  // Android-specific shared-code branch.
}

if (platform.isIos) {
  // iOS-specific shared-code branch.
}
```

Prefer platform-specific source sets for larger behavior differences.

## Android Permissions

The Android artifact declares the permissions required by toolkit features:

- `ACCESS_NETWORK_STATE`

Clipboard and share helpers use a `FileProvider` authority based on the consuming app id:
`${applicationId}.toolkit-clipboard`.

## Related Documentation

- Project overview: [../README.md](../README.md)
- Chinese usage guide: [README.zh.md](README.zh.md)
- Changelog: [../CHANGELOG.md](../CHANGELOG.md)
- Release process: [../docs/releasing.md](../docs/releasing.md)
