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
import androidx.compose.foundation.layout.ExperimentalLayoutApi

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
    description = item.description,
    code = item.code,
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = inputText,
      onValueChange = { inputText = it },
      modifier = Modifier.fillMaxWidth(),
      label = { Text(text = "输入以唤起软键盘") },
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
        Text(text = "开始监听")
      }
      OutlinedButton(onClick = {
        monitor.stopMonitoring()
        isObserving = false
      }) {
        Text(text = "停止监听")
      }
    }
    StatusText(value = latestStatus?.let { status ->
      "可见: ${status.isVisible}，高度: ${status.heightPx}px"
    })
  }
}
