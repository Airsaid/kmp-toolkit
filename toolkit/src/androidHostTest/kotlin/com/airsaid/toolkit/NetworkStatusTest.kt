package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkStatusTest {
  @Test
  fun disconnectedStatusHasNoPrimaryTransportByDefault() {
    val status = NetworkStatus(isConnected = false)
    assertEquals(emptySet(), status.transports)
    assertNull(status.primaryTransport)
  }

  @Test
  fun primaryTransportPrefersVpnOverUnderlyingTransport() {
    val status = NetworkStatus(
      isConnected = true,
      transports = setOf(NetworkTransport.WIFI, NetworkTransport.VPN),
    )
    assertEquals(NetworkTransport.VPN, status.primaryTransport)
  }

  @Test
  fun primaryTransportPrefersWifiOverCellular() {
    val status = NetworkStatus(
      isConnected = true,
      transports = setOf(NetworkTransport.CELLULAR, NetworkTransport.WIFI),
    )
    assertEquals(NetworkTransport.WIFI, status.primaryTransport)
  }

  @Test
  fun primaryTransportFallsBackByPriority() {
    assertEquals(
      NetworkTransport.CELLULAR,
      NetworkStatus(
        isConnected = true,
        transports = setOf(NetworkTransport.CELLULAR, NetworkTransport.ETHERNET),
      ).primaryTransport,
    )
    assertEquals(
      NetworkTransport.ETHERNET,
      NetworkStatus(
        isConnected = true,
        transports = setOf(NetworkTransport.ETHERNET, NetworkTransport.UNKNOWN),
      ).primaryTransport,
    )
    assertEquals(
      NetworkTransport.UNKNOWN,
      NetworkStatus(
        isConnected = true,
        transports = setOf(NetworkTransport.UNKNOWN),
      ).primaryTransport,
    )
  }
}
