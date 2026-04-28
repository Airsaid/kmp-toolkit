package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {

  @Test
  fun platformTypeIsAvailable() {
    val type = Toolkit.platformType()
    assertTrue(type.name.isNotBlank())
    assertTrue(type in PlatformType.values())
  }
}
