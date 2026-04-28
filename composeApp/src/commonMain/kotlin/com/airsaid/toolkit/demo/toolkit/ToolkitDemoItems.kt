package com.airsaid.toolkit.demo.toolkit

internal data class ToolkitDemoItem(
  val title: String,
  val route: String,
  val description: String,
  val code: String,
)

internal object ToolkitDemoItems {
  const val AppLifecycleRoute = "ToolkitAppLifecycle"
  const val AppInfoRoute = "ToolkitAppInfo"
  const val ClipboardRoute = "ToolkitClipboard"
  const val HapticRoute = "ToolkitHaptic"
  const val NetworkRoute = "ToolkitNetwork"
  const val SensorRoute = "ToolkitSensor"
  const val AppNavigatorRoute = "ToolkitAppNavigator"
  const val DeviceInfoRoute = "ToolkitDeviceInfo"
  const val KeyboardRoute = "ToolkitKeyboard"
  const val PlatformRoute = "ToolkitPlatform"
  const val ShareRoute = "ToolkitShare"
  const val FileRoute = "ToolkitFile"

  val all: List<ToolkitDemoItem> = listOf(
    ToolkitDemoItem(
      title = "应用生命周期监听",
      route = AppLifecycleRoute,
      description = "监听前后台与冷/热启动状态。",
      code = """
        val monitor = Toolkit.appLifecycleMonitor()
        monitor.startMonitoring()
        scope.launch {
          monitor.observeAppLifecycle().collect { status ->
            if (status.isInForeground) {

            }
          }
        }
        val status = monitor.getCurrentStatus()
        if (status.isFirstLaunch && status.lastStartType == AppStartType.COLD) {

        }
        monitor.stopMonitoring()
      """.trimIndent(),
    ),
    ToolkitDemoItem(
      title = "应用信息获取",
      route = AppInfoRoute,
      description = "读取应用包名、版本与构建号。",
      code = """
        val info = Toolkit.appInfo()
        println(info.packageName)
        println(info.versionName)
        println(info.buildNumber)
      """.trimIndent(),
    ),
    ToolkitDemoItem(
      title = "剪贴板读写与监听",
      route = ClipboardRoute,
      description = "写入、读取与监听剪贴板多类型内容变化。",
      code = """
        val clipboard = Toolkit.clipboard()
        clipboard.setText("hello")
        clipboard.setContents(
          listOf(
            ClipboardContent.RichText(
              text = "<b>Hello</b>",
              format = RichTextFormat.HTML,
              plainText = "Hello",
            ),
            ClipboardContent.Uri("https://example.com"),
          )
        )
        val snapshot = clipboard.getSnapshot()
        val text = clipboard.getText()
        scope.launch {
          clipboard.observeClipboard().collect { latest ->

          }
        }
        clipboard.clear()
      """.trimIndent(),
    ),
    ToolkitDemoItem(
      title = "触感反馈",
      route = HapticRoute,
      description = "触发成功、警告、错误与选择反馈。",
      code = """
        val haptics = Toolkit.hapticFeedback()
        if (haptics.isSupported()) {
          haptics.perform(HapticFeedbackType.SELECTION)
        }
        haptics.perform(HapticFeedbackType.SUCCESS)
        haptics.perform(HapticFeedbackType.WARNING)
        haptics.perform(HapticFeedbackType.ERROR)
      """.trimIndent(),
    ),
    ToolkitDemoItem(
      title = "网络状态监听",
      route = NetworkRoute,
      description = "监听网络连接状态与类型变化。",
      code = """
        val monitor = Toolkit.networkMonitor()
        monitor.startMonitoring()
        scope.launch {
          monitor.observeNetworkStatus().collect { status ->
            if (!status.isConnected) {

            }
          }
        }
        val status = monitor.getCurrentNetworkStatus()
        println(status.isConnected)
        println(status.type)
        val isWifi = status.type == NetworkType.WIFI
        monitor.stopMonitoring()
      """.trimIndent(),
    ),
    ToolkitDemoItem(
      title = "传感器监听",
      route = SensorRoute,
      description = "使用 SensorToolkit 监听加速度计、陀螺仪与设备姿态等传感器。",
      code = """
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
      """.trimIndent(),
    ),
    ToolkitDemoItem(
      title = "应用级跳转",
      route = AppNavigatorRoute,
      description = "跳转系统设置、应用详情、通知设置、邮件/拨号/短信、应用商店详情与链接。",
      code = """
        val navigator = Toolkit.appNavigator()
        val openedSystem = navigator.navigateToSystemSettings()
        val openedApp = navigator.navigateToAppDetails()
        val openedNotifications = navigator.navigateToNotificationSettings()
        val openedEmail = navigator.navigateToEmail(
          to = "support@example.com",
          subject = "反馈",
          body = "请描述你的问题",
        )
        val openedDial = navigator.navigateToDial("10086")
        val openedSms = navigator.navigateToSms(
          phone = "10086",
          body = "你好",
        )
        val openedStore = navigator.navigateToAppStoreDetails("com.example.app")
        val openedUrl = navigator.navigateToUrl("https://example.com")
      """.trimIndent(),
    ),
    ToolkitDemoItem(
      title = "文件选择与保存",
      route = FileRoute,
      description = "选择文件/目录、多选文件与保存文件。",
      code = """
        val files = Toolkit.fileToolkit()
        val picked = files.pickFile(
          FilePickerOptions(
            title = "选择文件",
            type = PlatformFileType.File(),
          )
        )
        val selected = files.pickFiles(
          FilePickerOptions(
            title = "多选文件",
            mode = FilePickerMode.Multiple(maxItems = 3),
          )
        )
        val directory = files.pickDirectory(
          DirectoryPickerOptions(title = "选择目录")
        )
        val saved = files.saveFile(
          FileSaveOptions(
            suggestedName = "document",
            extension = "txt",
          )
        )
      """.trimIndent(),
    ),
    ToolkitDemoItem(
      title = "系统分享",
      route = ShareRoute,
      description = "调起系统分享面板，分享文本、链接与图片。",
      code = """
        Toolkit.shareToolkit().share(
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
      """.trimIndent(),
    ),
    ToolkitDemoItem(
      title = "设备信息获取",
      route = DeviceInfoRoute,
      description = "读取设备型号、系统信息、屏幕与语言偏好。",
      code = """
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
        println(device.locale.current.tag)
        println(device.locale.preferred)
      """.trimIndent(),
    ),
    ToolkitDemoItem(
      title = "平台判断",
      route = PlatformRoute,
      description = "在 common 中判断 Platform 类型。",
      code = """
        when (Toolkit.platformType()) {
          PlatformType.ANDROID -> {

          }
          PlatformType.IOS -> {

          }
        }
      """.trimIndent(),
    ),
    ToolkitDemoItem(
      title = "键盘状态监听",
      route = KeyboardRoute,
      description = "监听软键盘可见性与高度变化。",
      code = """
        val keyboardMonitor = Toolkit.keyboardMonitor()
        keyboardMonitor.startMonitoring()
        scope.launch {
          keyboardMonitor.observeKeyboardStatus().collect { status ->
            if (status.isVisible) {

            }
          }
        }
        val status = keyboardMonitor.getCurrentStatus()
        println(status.isVisible)
        println(status.heightPx)
        keyboardMonitor.stopMonitoring()
      """.trimIndent(),
    ),
  )
}
