# kmp-toolkit

[中文说明](README.zh.md)

[![Maven Central](https://img.shields.io/maven-central/v/com.airsaid/toolkit.svg)](https://central.sonatype.com/artifact/com.airsaid/toolkit)
[![CI](https://img.shields.io/github/actions/workflow/status/Airsaid/kmp-toolkit/ci.yml?branch=main)](https://github.com/Airsaid/kmp-toolkit/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF.svg)

A Kotlin Multiplatform toolkit for common app and device capabilities on Android and iOS.

## Features

- App lifecycle monitoring.
- App and device information.
- Clipboard read/write and observation.
- Network state monitoring.
- Keyboard visibility monitoring.
- Haptic feedback.
- Sensor observation.
- App navigation helpers for settings, app details, URLs, email, dial, SMS, and store pages.
- System share sheet helpers.
- File picking, directory picking, and file save helpers.

## Installation

Add the dependency in `commonMain`:

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

Android apps must initialize the toolkit before using platform-backed APIs. iOS does not need an initialization call.

```kotlin
class DemoApp : Application() {
  override fun onCreate() {
    super.onCreate()
    Toolkit.initialize(applicationContext)
  }
}
```

## Quick Start

```kotlin
val appInfo = Toolkit.appInfo()
println(appInfo.versionName)

val device = Toolkit.deviceInfo()
println(device.systemVersion)

val networkMonitor = Toolkit.network()
networkMonitor.startMonitoring()

val clipboard = Toolkit.clipboard()
scope.launch {
  clipboard.setText("hello")
}

val haptics = Toolkit.haptics()
haptics.perform(HapticFeedbackType.SUCCESS)
```
Detailed usage guide: [toolkit/README.md](toolkit/README.md)

## Android Permissions

The Android artifact declares the permissions needed by toolkit features:

- `ACCESS_NETWORK_STATE`
- `ACCESS_WIFI_STATE`
- `VIBRATE`

Clipboard and share helpers use a `FileProvider` authority based on the consuming app id:
`${applicationId}.toolkit-clipboard`.

## Platforms

- Android: minSdk 24, compileSdk 36, JVM target 17
- iOS: iosArm64, iosX64, iosSimulatorArm64
- Kotlin: 2.2.21

## Change Log and Publishing

- Change log: [CHANGELOG.md](CHANGELOG.md)
- Release process: [docs/releasing.md](docs/releasing.md)

## License

Apache-2.0. See [LICENSE](LICENSE).
