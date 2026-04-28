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
import com.airsaid.toolkit.Toolkit
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitDeviceInfoScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.DeviceInfoRoute } }
  var deviceInfo by remember { mutableStateOf<String?>(null) }

  ToolkitDemoPage(
    description = item.description,
    code = item.code,
    modifier = modifier,
  ) {
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(onClick = {
        val device = Toolkit.deviceInfo()
        deviceInfo = buildString {
          append("型号: ").append(device.deviceModel).append("\n")
          append("系统: ").append(device.systemName).append(" ").append(device.systemVersion).append("\n")
          append("系统版本号: ").append(device.systemVersionCode).append("\n")
          append("厂商: ").append(device.manufacturer.manufacturer).append("\n")
          append("品牌: ").append(device.manufacturer.brand).append("\n")
          append("平板: ").append(device.deviceType.isTablet).append("\n")
          append("模拟器: ").append(device.deviceType.isEmulator).append("\n")
          append("屏幕 (px): ").append(device.screen.widthPx).append(" x ").append(device.screen.heightPx).append("\n")
          append("屏幕 (dp): ").append(device.screen.widthDp).append(" x ").append(device.screen.heightDp).append("\n")
          append("密度: ").append(device.screen.density).append(" / ").append(device.screen.densityDpi).append("dpi").append("\n")
          append("方向: ").append(if (device.screen.isLandscape) "横屏" else "竖屏").append("\n")
          append("时区: ").append(device.timeZone.id).append(" (").append(device.timeZone.offsetMinutes).append(" 分钟)").append("\n")
          append("当前语言: ").append(device.locale.current.tag).append("\n")
          append("首选语言: ").append(device.locale.preferred.joinToString { it.tag })
        }
      }) {
        Text(text = "读取设备信息")
      }
    }
    StatusText(value = deviceInfo)
  }
}
