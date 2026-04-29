package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {

  @Test
  fun currentPlatformIsAvailable() {
    val platform = Toolkit.currentPlatform()
    assertTrue(platform is PlatformType.Android || platform is PlatformType.Ios)
  }

  @Test
  fun platformChecksMatchCurrentPlatform() {
    val platform = Toolkit.currentPlatform()
    assertTrue(platform.isAndroid() || platform.isIos())
    assertTrue(platform.isAndroid() != platform.isIos())
  }
}
