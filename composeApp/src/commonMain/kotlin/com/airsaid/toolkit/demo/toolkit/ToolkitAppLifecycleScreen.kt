package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.AppLifecycleStatus
import com.airsaid.toolkit.Toolkit
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitAppLifecycleScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.AppLifecycleRoute } }
  val monitor = remember { Toolkit.appLifecycleMonitor() }
  var isObserving by remember { mutableStateOf(false) }
  var latestStatus by remember { mutableStateOf<AppLifecycleStatus?>(null) }

  LaunchedEffect(isObserving) {
    if (!isObserving) return@LaunchedEffect
    monitor.observeAppLifecycle().collect { status ->
      latestStatus = status
    }
  }

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
      buildString {
        append("前台: ").append(status.isInForeground)
        append("，可见: ").append(status.isVisible)
        append("，首次启动: ").append(status.isFirstLaunch)
        append("，冷启动: ").append(status.coldStartCount)
        append("，热启动: ").append(status.hotStartCount)
        append("，最近类型: ").append(status.lastStartType ?: "-")
      }
    })
  }
}
