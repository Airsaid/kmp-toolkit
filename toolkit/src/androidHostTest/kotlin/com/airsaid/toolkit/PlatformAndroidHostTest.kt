package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformAndroidHostTest {

  @Test
  fun androidHostReportsAndroidPlatform() {
    assertEquals(PlatformType.Android, Toolkit.platform)
  }
}
