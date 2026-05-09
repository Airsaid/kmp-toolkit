package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.DeviceInfo
import com.airsaid.toolkit.DisplayInfo
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.action_read_device_info
import com.airsaid.toolkit.demo.resources.device_brand
import com.airsaid.toolkit.demo.resources.device_current_language
import com.airsaid.toolkit.demo.resources.device_density
import com.airsaid.toolkit.demo.resources.device_emulator
import com.airsaid.toolkit.demo.resources.device_landscape
import com.airsaid.toolkit.demo.resources.device_manufacturer
import com.airsaid.toolkit.demo.resources.device_model
import com.airsaid.toolkit.demo.resources.device_orientation
import com.airsaid.toolkit.demo.resources.device_portrait
import com.airsaid.toolkit.demo.resources.device_preferred_languages
import com.airsaid.toolkit.demo.resources.device_screen_logical
import com.airsaid.toolkit.demo.resources.device_screen_px
import com.airsaid.toolkit.demo.resources.device_system
import com.airsaid.toolkit.demo.resources.device_system_version_code
import com.airsaid.toolkit.demo.resources.device_tablet
import com.airsaid.toolkit.demo.resources.device_time_zone
import com.airsaid.toolkit.demo.resources.device_time_zone_value
import com.airsaid.toolkit.demo.resources.device_window_logical
import com.airsaid.toolkit.demo.resources.device_window_px
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitDeviceInfoScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.DeviceInfoRoute } }
  var deviceInfo by remember { mutableStateOf<DeviceInfo?>(null) }

  ToolkitDemoPage(
    descriptionRes = item.descriptionRes,
    codeRes = item.codeRes,
    modifier = modifier,
  ) {
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(onClick = {
        deviceInfo = Toolkit.deviceInfo()
      }) {
        Text(text = stringResource(Res.string.action_read_device_info))
      }
    }
    StatusText(value = deviceInfo?.let { device -> buildDeviceInfoText(device) })
  }
}

@Composable
private fun buildDeviceInfoText(device: DeviceInfo): String {
  val rows = listOf(
    stringResource(Res.string.device_model) to device.deviceModel,
    stringResource(Res.string.device_system) to "${device.systemName} ${device.systemVersion}",
    stringResource(Res.string.device_system_version_code) to device.systemVersionCode?.toString().orEmpty(),
    stringResource(Res.string.device_manufacturer) to device.manufacturer.manufacturer,
    stringResource(Res.string.device_brand) to device.manufacturer.brand,
    stringResource(Res.string.device_tablet) to device.deviceType.isTablet.toString(),
    stringResource(Res.string.device_emulator) to device.deviceType.isEmulator.toString(),
    stringResource(Res.string.device_window_px) to formatDisplayPx(device.window),
    stringResource(Res.string.device_window_logical) to formatDisplayLogical(device.window),
    stringResource(Res.string.device_screen_px) to "${device.screen.widthPx} x ${device.screen.heightPx}",
    stringResource(Res.string.device_screen_logical) to formatDisplayLogical(device.screen),
    stringResource(Res.string.device_density) to "${device.screen.density} / ${device.screen.densityDpi}dpi",
    stringResource(Res.string.device_orientation) to if (device.screen.isLandscape) {
      stringResource(Res.string.device_landscape)
    } else {
      stringResource(Res.string.device_portrait)
    },
    stringResource(Res.string.device_time_zone) to stringResource(
      Res.string.device_time_zone_value,
      device.timeZone.id,
      device.timeZone.offsetMinutes,
    ),
    stringResource(Res.string.device_current_language) to device.locale.current.languageTag,
    stringResource(Res.string.device_preferred_languages) to device.locale.preferred.joinToString { it.languageTag },
  )
  return rows.joinToString("\n") { (label, value) ->
    "$label: ${value.ifBlank { "-" }}"
  }
}

private fun formatDisplayPx(display: DisplayInfo?): String {
  return display?.let { "${it.widthPx} x ${it.heightPx}" }.orEmpty()
}

private fun formatDisplayLogical(display: DisplayInfo?): String {
  return display?.let { "${it.widthLogical} x ${it.heightLogical}" }.orEmpty()
}
