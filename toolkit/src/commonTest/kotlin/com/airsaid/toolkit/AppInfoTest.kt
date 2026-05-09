package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppInfoTest {

  @Test
  fun appInfoKeepsNullableFields() {
    val info = AppInfo(packageName = "com.airsaid.demo")

    assertEquals("com.airsaid.demo", info.packageName)
    assertNull(info.appName)
    assertNull(info.versionName)
    assertNull(info.buildNumber)
  }

  @Test
  fun appInfoKeepsRuntimeMetadata() {
    val info = AppInfo(
      packageName = "com.airsaid.demo",
      appName = "Toolkit",
      versionName = "1.0.0",
      buildNumber = "100",
    )

    assertEquals("com.airsaid.demo", info.packageName)
    assertEquals("Toolkit", info.appName)
    assertEquals("1.0.0", info.versionName)
    assertEquals("100", info.buildNumber)
  }
}
