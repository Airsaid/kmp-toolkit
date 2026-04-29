package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.NetworkStatus
import com.airsaid.toolkit.NetworkType
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.network_cellular_connected
import com.airsaid.toolkit.demo.resources.network_checking
import com.airsaid.toolkit.demo.resources.network_ethernet_connected
import com.airsaid.toolkit.demo.resources.network_not_connected
import com.airsaid.toolkit.demo.resources.network_vpn_connected
import com.airsaid.toolkit.demo.resources.network_wifi_connected
import org.jetbrains.compose.resources.stringResource

/**
 * @author airsaid
 */
@Composable
fun NetworkMonitorScreen(modifier: Modifier = Modifier) {
  val networkMonitor = remember { Toolkit.network() }
  val networkStatus by networkMonitor.observeNetworkStatus()
    .collectAsState(initial = NetworkStatus(false, NetworkType.UNKNOWN))

  Column(modifier = modifier) {
    NetworkStatusBar(networkStatus)
  }

  DisposableEffect(networkMonitor) {
    networkMonitor.startMonitoring()
    onDispose {
      networkMonitor.stopMonitoring()
    }
  }
}

@Composable
private fun NetworkStatusBar(status: NetworkStatus) {
  val backgroundColor = when {
    !status.isConnected -> Color.Red
    status.type == NetworkType.WIFI -> Color.Green
    status.type == NetworkType.CELLULAR -> Color.Yellow
    else -> Color.Gray
  }

  val textColor = when {
    !status.isConnected -> Color.White
    status.type == NetworkType.WIFI -> Color.Black
    status.type == NetworkType.CELLULAR -> Color.Black
    else -> Color.White
  }

  val text = when (status.type) {
    NetworkType.WIFI -> stringResource(Res.string.network_wifi_connected)
    NetworkType.CELLULAR -> stringResource(Res.string.network_cellular_connected)
    NetworkType.ETHERNET -> stringResource(Res.string.network_ethernet_connected)
    NetworkType.VPN -> stringResource(Res.string.network_vpn_connected)
    NetworkType.NONE -> stringResource(Res.string.network_not_connected)
    NetworkType.UNKNOWN -> stringResource(Res.string.network_checking)
  }

  Surface(
    color = backgroundColor,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(8.dp),
      color = textColor
    )
  }
}
