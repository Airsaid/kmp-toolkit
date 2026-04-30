# kmp-toolkit

[English](README.md)

[![Maven Central](https://img.shields.io/maven-central/v/com.airsaid/toolkit.svg)](https://central.sonatype.com/artifact/com.airsaid/toolkit)
[![CI](https://img.shields.io/github/actions/workflow/status/Airsaid/kmp-toolkit/ci.yml?branch=main)](https://github.com/Airsaid/kmp-toolkit/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF.svg)

一组支持 Android 与 iOS 的 Kotlin Multiplatform 系统能力封装。

## 特性

- 应用生命周期监听。
- 应用信息与设备信息获取。
- 剪贴板读写与监听。
- 网络状态监听。
- 键盘可见性监听。
- 触感反馈。
- 传感器监听。
- 系统设置、应用详情、链接、邮件、拨号、短信和商店页跳转。
- 系统分享面板。
- 文件选择、目录选择与文件保存。

## 安装

在 `commonMain` 中添加依赖：

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

Android 端需要先初始化后才能使用平台能力，iOS 端无需调用：

```kotlin
class DemoApp : Application() {
  override fun onCreate() {
    super.onCreate()
    Toolkit.initialize(applicationContext)
  }
}
```

## 快速开始

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
详细使用文档：[toolkit/README.zh.md](toolkit/README.zh.md)

## Android 权限

Android 产物声明了 toolkit 功能所需权限：

- `ACCESS_NETWORK_STATE`
- `ACCESS_WIFI_STATE`
- `VIBRATE`

剪贴板和分享能力使用基于接入方应用 id 的 `FileProvider` authority：
`${applicationId}.toolkit-clipboard`。

## 平台与版本

- Android: minSdk 24, compileSdk 36, JVM target 17
- iOS: iosArm64, iosX64, iosSimulatorArm64
- Kotlin: 2.2.21

## 变更记录与发布

- 变更记录：[CHANGELOG.md](CHANGELOG.md)
- 发布流程：[docs/releasing.md](docs/releasing.md)

## License

Apache-2.0. See [LICENSE](LICENSE).
