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
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.action_start_monitoring
import com.airsaid.toolkit.demo.resources.action_stop_monitoring
import com.airsaid.toolkit.demo.resources.network_cellular_connected
import com.airsaid.toolkit.demo.resources.network_checking
import com.airsaid.toolkit.demo.resources.network_ethernet_connected
import com.airsaid.toolkit.demo.resources.network_not_connected
import com.airsaid.toolkit.demo.resources.network_vpn_connected
import com.airsaid.toolkit.demo.resources.network_wifi_connected
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolkitNetworkScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.NetworkRoute } }
  val monitor = remember { Toolkit.network() }
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
    NetworkType.WIFI -> stringResource(Res.string.network_wifi_connected)
    NetworkType.CELLULAR -> stringResource(Res.string.network_cellular_connected)
    NetworkType.ETHERNET -> stringResource(Res.string.network_ethernet_connected)
    NetworkType.VPN -> stringResource(Res.string.network_vpn_connected)
    NetworkType.NONE -> stringResource(Res.string.network_not_connected)
    NetworkType.UNKNOWN -> stringResource(Res.string.network_checking)
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
