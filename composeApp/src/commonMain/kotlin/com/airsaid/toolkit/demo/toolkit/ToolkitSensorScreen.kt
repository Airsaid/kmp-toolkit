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
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.action_start_monitoring
import com.airsaid.toolkit.demo.resources.action_stop_monitoring
import com.airsaid.toolkit.demo.resources.sensor_accelerometer
import com.airsaid.toolkit.demo.resources.sensor_ambient_temperature
import com.airsaid.toolkit.demo.resources.sensor_available
import com.airsaid.toolkit.demo.resources.sensor_availability_format
import com.airsaid.toolkit.demo.resources.sensor_axis_value_format
import com.airsaid.toolkit.demo.resources.sensor_barometer
import com.airsaid.toolkit.demo.resources.sensor_current_format
import com.airsaid.toolkit.demo.resources.sensor_device_motion
import com.airsaid.toolkit.demo.resources.sensor_game_rotation_vector
import com.airsaid.toolkit.demo.resources.sensor_geomagnetic_rotation_vector
import com.airsaid.toolkit.demo.resources.sensor_gravity
import com.airsaid.toolkit.demo.resources.sensor_gyroscope
import com.airsaid.toolkit.demo.resources.sensor_light
import com.airsaid.toolkit.demo.resources.sensor_linear_acceleration
import com.airsaid.toolkit.demo.resources.sensor_magnetometer
import com.airsaid.toolkit.demo.resources.sensor_motion_detect
import com.airsaid.toolkit.demo.resources.sensor_proximity
import com.airsaid.toolkit.demo.resources.sensor_relative_humidity
import com.airsaid.toolkit.demo.resources.sensor_rotation_vector
import com.airsaid.toolkit.demo.resources.sensor_sampling_not_observing
import com.airsaid.toolkit.demo.resources.sensor_sampling_observing
import com.airsaid.toolkit.demo.resources.sensor_sampling_status_format
import com.airsaid.toolkit.demo.resources.sensor_select_format
import com.airsaid.toolkit.demo.resources.sensor_significant_motion
import com.airsaid.toolkit.demo.resources.sensor_stationary_detect
import com.airsaid.toolkit.demo.resources.sensor_step_counter
import com.airsaid.toolkit.demo.resources.sensor_step_detector
import com.airsaid.toolkit.demo.resources.sensor_tilt_detector
import com.airsaid.toolkit.demo.resources.sensor_unavailable
import com.airsaid.toolkit.demo.resources.sensor_unavailable_reason
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitSensorScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.SensorRoute } }
  val toolkit = remember { Toolkit.sensors() }
  var selectedType by remember { mutableStateOf(SensorType.ACCELEROMETER) }
  var latestEvent by remember { mutableStateOf<SensorEvent?>(null) }
  var availability by remember { mutableStateOf<SensorAvailability?>(null) }
  var isObserving by remember { mutableStateOf(false) }
  var isMenuExpanded by remember { mutableStateOf(false) }
  val selectedDisplayName = stringResource(selectedType.displayNameRes())

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
    descriptionRes = item.descriptionRes,
    codeRes = item.codeRes,
    modifier = modifier,
  ) {
    Button(
      onClick = { isMenuExpanded = true },
    ) {
      Text(text = stringResource(Res.string.sensor_select_format, selectedDisplayName))
    }
    DropdownMenu(
      expanded = isMenuExpanded,
      onDismissRequest = { isMenuExpanded = false },
    ) {
      SensorType.entries.forEach { type ->
        DropdownMenuItem(
          text = { Text(text = stringResource(type.displayNameRes())) },
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
        Text(text = stringResource(Res.string.action_start_monitoring))
      }
      OutlinedButton(onClick = {
        isObserving = false
        toolkit.stop(selectedType)
      }) {
        Text(text = stringResource(Res.string.action_stop_monitoring))
      }
    }

    StatusText(value = stringResource(Res.string.sensor_current_format, selectedDisplayName))
    StatusText(value = stringResource(Res.string.sensor_availability_format, availability.toDisplayText()))
    StatusText(
      value = stringResource(
        Res.string.sensor_sampling_status_format,
        if (isObserving) {
          stringResource(Res.string.sensor_sampling_observing)
        } else {
          stringResource(Res.string.sensor_sampling_not_observing)
        },
      ),
    )
    StatusText(value = latestEvent.axisValue("X", 0))
    StatusText(value = latestEvent.axisValue("Y", 1))
    StatusText(value = latestEvent.axisValue("Z", 2))
  }
}

private fun SensorType.displayNameRes(): StringResource {
  return when (this) {
    SensorType.ACCELEROMETER -> Res.string.sensor_accelerometer
    SensorType.GYROSCOPE -> Res.string.sensor_gyroscope
    SensorType.MAGNETOMETER -> Res.string.sensor_magnetometer
    SensorType.BAROMETER -> Res.string.sensor_barometer
    SensorType.STEP_COUNTER -> Res.string.sensor_step_counter
    SensorType.STEP_DETECTOR -> Res.string.sensor_step_detector
    SensorType.DEVICE_MOTION -> Res.string.sensor_device_motion
    SensorType.LIGHT -> Res.string.sensor_light
    SensorType.PROXIMITY -> Res.string.sensor_proximity
    SensorType.GRAVITY -> Res.string.sensor_gravity
    SensorType.LINEAR_ACCELERATION -> Res.string.sensor_linear_acceleration
    SensorType.ROTATION_VECTOR -> Res.string.sensor_rotation_vector
    SensorType.GAME_ROTATION_VECTOR -> Res.string.sensor_game_rotation_vector
    SensorType.GEOMAGNETIC_ROTATION_VECTOR -> Res.string.sensor_geomagnetic_rotation_vector
    SensorType.TILT_DETECTOR -> Res.string.sensor_tilt_detector
    SensorType.SIGNIFICANT_MOTION -> Res.string.sensor_significant_motion
    SensorType.MOTION_DETECT -> Res.string.sensor_motion_detect
    SensorType.STATIONARY_DETECT -> Res.string.sensor_stationary_detect
    SensorType.AMBIENT_TEMPERATURE -> Res.string.sensor_ambient_temperature
    SensorType.RELATIVE_HUMIDITY -> Res.string.sensor_relative_humidity
  }
}

@Composable
private fun SensorAvailability?.toDisplayText(): String {
  val unavailableReason = this?.reason
  return when {
    this == null -> "-"
    isAvailable -> stringResource(Res.string.sensor_available)
    unavailableReason.isNullOrBlank() -> stringResource(Res.string.sensor_unavailable)
    else -> stringResource(Res.string.sensor_unavailable_reason, unavailableReason)
  }
}

@Composable
private fun SensorEvent?.axisValue(label: String, index: Int): String? {
  val value = this?.values?.getOrNull(index) ?: return null
  return stringResource(Res.string.sensor_axis_value_format, label, value)
}
