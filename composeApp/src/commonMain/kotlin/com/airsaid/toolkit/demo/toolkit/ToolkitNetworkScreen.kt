package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.NetworkStatus
import com.airsaid.toolkit.NetworkType
import com.airsaid.toolkit.Toolkit
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitNetworkScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.NetworkRoute } }
  val monitor = remember { Toolkit.networkMonitor() }
  var latestStatus by remember { mutableStateOf<NetworkStatus?>(null) }
  var isObserving by remember { mutableStateOf(false) }

  LaunchedEffect(isObserving) {
    if (!isObserving) return@LaunchedEffect
    monitor.observeNetworkStatus().collect { status ->
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
    NetworkStatusBar(status = latestStatus)
  }
}

@Composable
private fun NetworkStatusBar(status: NetworkStatus?) {
  val resolvedStatus = status ?: NetworkStatus(false, NetworkType.UNKNOWN)
  val backgroundColor = when {
    !resolvedStatus.isConnected -> Color.Red
    resolvedStatus.type == NetworkType.WIFI -> Color.Green
    resolvedStatus.type == NetworkType.CELLULAR -> Color.Yellow
    else -> Color.Gray
  }

  val textColor = when {
    !resolvedStatus.isConnected -> Color.White
    resolvedStatus.type == NetworkType.WIFI -> Color.Black
    resolvedStatus.type == NetworkType.CELLULAR -> Color.Black
    else -> Color.White
  }

  val text = when (resolvedStatus.type) {
    NetworkType.WIFI -> "WiFi 已连接"
    NetworkType.CELLULAR -> "移动数据"
    NetworkType.ETHERNET -> "有线连接"
    NetworkType.VPN -> "VPN 连接"
    NetworkType.NONE -> "无网络连接"
    NetworkType.UNKNOWN -> "检测中..."
  }

  Surface(
    color = backgroundColor,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(8.dp),
      color = textColor
    )
  }
}
