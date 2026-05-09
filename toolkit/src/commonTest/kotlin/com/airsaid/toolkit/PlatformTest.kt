package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {

  @Test
  fun platformIsAvailable() {
    val platform = Toolkit.platform
    assertTrue(platform is PlatformType.Android || platform is PlatformType.Ios)
  }

  @Test
  fun platformChecksMatchCurrentPlatform() {
    val platform = Toolkit.platform
    assertTrue(platform.isAndroid || platform.isIos)
    assertTrue(platform.isAndroid != platform.isIos)
  }
}
