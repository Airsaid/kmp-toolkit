package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.SensorAvailability
import com.airsaid.toolkit.SensorDelay
import com.airsaid.toolkit.SensorEvent
import com.airsaid.toolkit.SensorOptions
import com.airsaid.toolkit.SensorType
import com.airsaid.toolkit.Toolkit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitSensorScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.SensorRoute } }
  val toolkit = remember { Toolkit.sensorToolkit() }
  var selectedType by remember { mutableStateOf(SensorType.ACCELEROMETER) }
  var latestEvent by remember { mutableStateOf<SensorEvent?>(null) }
  var availability by remember { mutableStateOf<SensorAvailability?>(null) }
  var isObserving by remember { mutableStateOf(false) }
  var isMenuExpanded by remember { mutableStateOf(false) }
  val selectedDisplayName = selectedType.displayName()

  LaunchedEffect(selectedType) {
    availability = toolkit.isAvailable(selectedType)
    latestEvent = null
  }

  LaunchedEffect(selectedType, isObserving) {
    if (!isObserving) return@LaunchedEffect
    toolkit.observe(
      type = selectedType,
      options = SensorOptions(
        delay = SensorDelay.UI,
      ),
    ).collect { event ->
      latestEvent = event
    }
  }

  DisposableEffect(selectedType) {
    onDispose {
      toolkit.stop(selectedType)
    }
  }

  ToolkitDemoPage(
    description = item.description,
    code = item.code,
    modifier = modifier,
  ) {
    Button(
      onClick = { isMenuExpanded = true },
    ) {
      Text(text = "选择传感器: $selectedDisplayName")
    }
    DropdownMenu(
      expanded = isMenuExpanded,
      onDismissRequest = { isMenuExpanded = false },
    ) {
      SensorType.entries.forEach { type ->
        DropdownMenuItem(
          text = { Text(text = type.displayName()) },
          onClick = {
            selectedType = type
            isObserving = false
            isMenuExpanded = false
          },
        )
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(onClick = {
        if (availability?.isAvailable == true) {
          isObserving = true
        }
      }) {
        Text(text = "开始监听")
      }
      OutlinedButton(onClick = {
        isObserving = false
        toolkit.stop(selectedType)
      }) {
        Text(text = "停止监听")
      }
    }

    StatusText(value = "当前传感器: $selectedDisplayName")
    StatusText(value = "可用性: ${availability.toDisplayText()}")
    StatusText(value = "采样状态: ${if (isObserving) "监听中" else "未监听"}")
    StatusText(value = latestEvent.axisValue("X", 0))
    StatusText(value = latestEvent.axisValue("Y", 1))
    StatusText(value = latestEvent.axisValue("Z", 2))
  }
}

private fun SensorType.displayName(): String {
  return when (this) {
    SensorType.ACCELEROMETER -> "加速度计"
    SensorType.GYROSCOPE -> "陀螺仪"
    SensorType.MAGNETOMETER -> "磁力计"
    SensorType.BAROMETER -> "气压计"
    SensorType.STEP_COUNTER -> "计步"
    SensorType.STEP_DETECTOR -> "步伐检测"
    SensorType.DEVICE_MOTION -> "设备姿态"
    SensorType.LIGHT -> "光线"
    SensorType.PROXIMITY -> "距离"
    SensorType.GRAVITY -> "重力"
    SensorType.LINEAR_ACCELERATION -> "线性加速度"
    SensorType.ROTATION_VECTOR -> "旋转向量"
    SensorType.GAME_ROTATION_VECTOR -> "游戏旋转向量"
    SensorType.GEOMAGNETIC_ROTATION_VECTOR -> "地磁旋转向量"
    SensorType.TILT_DETECTOR -> "倾斜检测"
    SensorType.SIGNIFICANT_MOTION -> "显著运动"
    SensorType.MOTION_DETECT -> "运动检测"
    SensorType.STATIONARY_DETECT -> "静止检测"
    SensorType.AMBIENT_TEMPERATURE -> "环境温度"
    SensorType.RELATIVE_HUMIDITY -> "相对湿度"
  }
}

private fun SensorAvailability?.toDisplayText(): String {
  return when {
    this == null -> "-"
    isAvailable -> "可用"
    else -> "不可用" + (reason?.let { ": $it" } ?: "")
  }
}

private fun SensorEvent?.axisValue(label: String, index: Int): String? {
  val value = this?.values?.getOrNull(index) ?: return null
  return "$label: $value"
}
