package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkStatusTest {
  @Test
  fun defaultTypeIsUnknown() {
    val status = NetworkStatus(isConnected = true)
    assertEquals(NetworkType.UNKNOWN, status.type)
  }

  @Test
  fun explicitNoneIsPreserved() {
    val status = NetworkStatus(isConnected = false, type = NetworkType.NONE)
    assertEquals(NetworkType.NONE, status.type)
  }
}
