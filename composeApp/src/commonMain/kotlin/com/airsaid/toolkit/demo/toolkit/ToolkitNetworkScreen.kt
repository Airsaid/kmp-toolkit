package com.airsaid.toolkit.demo.toolkit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airsaid.toolkit.NetworkStatus
import com.airsaid.toolkit.NetworkTransport
import com.airsaid.toolkit.Toolkit
import com.airsaid.toolkit.demo.resources.Res
import com.airsaid.toolkit.demo.resources.network_cellular_connected
import com.airsaid.toolkit.demo.resources.network_checking
import com.airsaid.toolkit.demo.resources.network_ethernet_connected
import com.airsaid.toolkit.demo.resources.network_not_connected
import com.airsaid.toolkit.demo.resources.network_vpn_connected
import com.airsaid.toolkit.demo.resources.network_wifi_connected
import org.jetbrains.compose.resources.stringResource

@Composable
fun ToolkitNetworkScreen(modifier: Modifier = Modifier) {
  val item = remember { ToolkitDemoItems.all.first { it.route == ToolkitDemoItems.NetworkRoute } }
  val monitor = remember { Toolkit.network() }
  val latestStatus by monitor.observeNetworkStatus()
    .collectAsState(initial = NetworkStatus(isConnected = false))

  ToolkitDemoPage(
    descriptionRes = item.descriptionRes,
    codeRes = item.codeRes,
    modifier = modifier,
  ) {
    NetworkStatusBar(status = latestStatus)
  }
}

@Composable
private fun NetworkStatusBar(status: NetworkStatus) {
  val primaryTransport = status.primaryTransport
  val backgroundColor = when {
    !status.isConnected -> Color.Red
    primaryTransport == NetworkTransport.WIFI -> Color.Green
    primaryTransport == NetworkTransport.CELLULAR -> Color.Yellow
    else -> Color.Gray
  }

  val textColor = when {
    !status.isConnected -> Color.White
    primaryTransport == NetworkTransport.WIFI -> Color.Black
    primaryTransport == NetworkTransport.CELLULAR -> Color.Black
    else -> Color.White
  }

  val text = when {
    !status.isConnected -> stringResource(Res.string.network_not_connected)
    primaryTransport == NetworkTransport.WIFI -> stringResource(Res.string.network_wifi_connected)
    primaryTransport == NetworkTransport.CELLULAR -> {
      stringResource(Res.string.network_cellular_connected)
    }
    primaryTransport == NetworkTransport.ETHERNET -> {
      stringResource(Res.string.network_ethernet_connected)
    }
    primaryTransport == NetworkTransport.VPN -> stringResource(Res.string.network_vpn_connected)
    else -> stringResource(Res.string.network_checking)
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
