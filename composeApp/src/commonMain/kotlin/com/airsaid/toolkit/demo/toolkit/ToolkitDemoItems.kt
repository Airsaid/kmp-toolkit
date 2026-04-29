package com.airsaid.toolkit.demo.toolkit

import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.demo_app_info_code
import com.airsaid.toolkit.demo.resources.demo_app_info_description
import com.airsaid.toolkit.demo.resources.demo_app_info_title
import com.airsaid.toolkit.demo.resources.demo_app_lifecycle_code
import com.airsaid.toolkit.demo.resources.demo_app_lifecycle_description
import com.airsaid.toolkit.demo.resources.demo_app_lifecycle_title
import com.airsaid.toolkit.demo.resources.demo_app_navigator_code
import com.airsaid.toolkit.demo.resources.demo_app_navigator_description
import com.airsaid.toolkit.demo.resources.demo_app_navigator_title
import com.airsaid.toolkit.demo.resources.demo_clipboard_code
import com.airsaid.toolkit.demo.resources.demo_clipboard_description
import com.airsaid.toolkit.demo.resources.demo_clipboard_title
import com.airsaid.toolkit.demo.resources.demo_device_info_code
import com.airsaid.toolkit.demo.resources.demo_device_info_description
import com.airsaid.toolkit.demo.resources.demo_device_info_title
import com.airsaid.toolkit.demo.resources.demo_file_code
import com.airsaid.toolkit.demo.resources.demo_file_description
import com.airsaid.toolkit.demo.resources.demo_file_title
import com.airsaid.toolkit.demo.resources.demo_haptic_code
import com.airsaid.toolkit.demo.resources.demo_haptic_description
import com.airsaid.toolkit.demo.resources.demo_haptic_title
import com.airsaid.toolkit.demo.resources.demo_keyboard_code
import com.airsaid.toolkit.demo.resources.demo_keyboard_description
import com.airsaid.toolkit.demo.resources.demo_keyboard_title
import com.airsaid.toolkit.demo.resources.demo_network_code
import com.airsaid.toolkit.demo.resources.demo_network_description
import com.airsaid.toolkit.demo.resources.demo_network_title
import com.airsaid.toolkit.demo.resources.demo_platform_code
import com.airsaid.toolkit.demo.resources.demo_platform_description
import com.airsaid.toolkit.demo.resources.demo_platform_title
import com.airsaid.toolkit.demo.resources.demo_sensor_code
import com.airsaid.toolkit.demo.resources.demo_sensor_description
import com.airsaid.toolkit.demo.resources.demo_sensor_title
import com.airsaid.toolkit.demo.resources.demo_share_code
import com.airsaid.toolkit.demo.resources.demo_share_description
import com.airsaid.toolkit.demo.resources.demo_share_title
import org.jetbrains.compose.resources.StringResource

internal data class ToolkitDemoItem(
  val titleRes: StringResource,
  val route: String,
  val descriptionRes: StringResource,
  val codeRes: StringResource,
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
      titleRes = Res.string.demo_app_lifecycle_title,
      route = AppLifecycleRoute,
      descriptionRes = Res.string.demo_app_lifecycle_description,
      codeRes = Res.string.demo_app_lifecycle_code,
    ),
    ToolkitDemoItem(
      titleRes = Res.string.demo_app_info_title,
      route = AppInfoRoute,
      descriptionRes = Res.string.demo_app_info_description,
      codeRes = Res.string.demo_app_info_code,
    ),
    ToolkitDemoItem(
      titleRes = Res.string.demo_clipboard_title,
      route = ClipboardRoute,
      descriptionRes = Res.string.demo_clipboard_description,
      codeRes = Res.string.demo_clipboard_code,
    ),
    ToolkitDemoItem(
      titleRes = Res.string.demo_haptic_title,
      route = HapticRoute,
      descriptionRes = Res.string.demo_haptic_description,
      codeRes = Res.string.demo_haptic_code,
    ),
    ToolkitDemoItem(
      titleRes = Res.string.demo_network_title,
      route = NetworkRoute,
      descriptionRes = Res.string.demo_network_description,
      codeRes = Res.string.demo_network_code,
    ),
    ToolkitDemoItem(
      titleRes = Res.string.demo_sensor_title,
      route = SensorRoute,
      descriptionRes = Res.string.demo_sensor_description,
      codeRes = Res.string.demo_sensor_code,
    ),
    ToolkitDemoItem(
      titleRes = Res.string.demo_app_navigator_title,
      route = AppNavigatorRoute,
      descriptionRes = Res.string.demo_app_navigator_description,
      codeRes = Res.string.demo_app_navigator_code,
    ),
    ToolkitDemoItem(
      titleRes = Res.string.demo_file_title,
      route = FileRoute,
      descriptionRes = Res.string.demo_file_description,
      codeRes = Res.string.demo_file_code,
    ),
    ToolkitDemoItem(
      titleRes = Res.string.demo_share_title,
      route = ShareRoute,
      descriptionRes = Res.string.demo_share_description,
      codeRes = Res.string.demo_share_code,
    ),
    ToolkitDemoItem(
      titleRes = Res.string.demo_device_info_title,
      route = DeviceInfoRoute,
      descriptionRes = Res.string.demo_device_info_description,
      codeRes = Res.string.demo_device_info_code,
    ),
    ToolkitDemoItem(
      titleRes = Res.string.demo_platform_title,
      route = PlatformRoute,
      descriptionRes = Res.string.demo_platform_description,
      codeRes = Res.string.demo_platform_code,
    ),
    ToolkitDemoItem(
      titleRes = Res.string.demo_keyboard_title,
      route = KeyboardRoute,
      descriptionRes = Res.string.demo_keyboard_description,
      codeRes = Res.string.demo_keyboard_code,
    ),
  )
}
