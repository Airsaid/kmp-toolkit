package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.airsaid.toolkit.KeyboardStatus
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.action_start_monitoring
import com.airsaid.toolkit.demo.resources.action_stop_monitoring
import com.airsaid.toolkit.demo.resources.keyboard_input_label
import com.airsaid.toolkit.demo.resources.keyboard_status_format
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitKeyboardScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.KeyboardRoute } }
  val monitor = remember { Toolkit.keyboardMonitor() }
  var latestStatus by remember { mutableStateOf<KeyboardStatus?>(null) }
  var isObserving by remember { mutableStateOf(false) }
  var inputText by remember { mutableStateOf("") }

  LaunchedEffect(isObserving) {
    if (!isObserving) return@LaunchedEffect
    monitor.observeKeyboardStatus().collect { status ->
      latestStatus = status
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      monitor.stopMonitoring()
    }
  }

  ToolkitDemoPage(
    descriptionRes = item.descriptionRes,
    codeRes = item.codeRes,
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = inputText,
      onValueChange = { inputText = it },
      modifier = Modifier.fillMaxWidth(),
      label = { Text(text = stringResource(Res.string.keyboard_input_label)) },
    )
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(onClick = {
        monitor.startMonitoring()
        isObserving = true
      }) {
        Text(text = stringResource(Res.string.action_start_monitoring))
      }
      OutlinedButton(onClick = {
        monitor.stopMonitoring()
        isObserving = false
      }) {
        Text(text = stringResource(Res.string.action_stop_monitoring))
      }
    }
    StatusText(value = latestStatus?.let { status ->
      stringResource(Res.string.keyboard_status_format, status.isVisible, status.heightPx)
    })
  }
}
