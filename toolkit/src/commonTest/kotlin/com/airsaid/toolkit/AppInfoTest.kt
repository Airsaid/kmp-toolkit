package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppInfoTest {

  @Test
  fun buildKonfigFieldsAreAvailable() {
    assertTrue(BuildKonfig.BUILD_TYPE.isNotBlank())
    assertTrue(BuildKonfig.BUILD_TIME.isNotBlank())
    assertTrue(BuildKonfig.BUILD_TIME.contains("T"))
  }

  @Test
  fun appInfoKeepsNewFields() {
    val info = AppInfo(
      packageName = "com.airsaid.demo",
      appName = "Toolkit",
      versionName = "1.0.0",
      buildNumber = "100",
      buildType = "debug",
      buildTime = "2026-01-06T00:00:00Z",
    )
    assertEquals("Toolkit", info.appName)
    assertEquals("debug", info.buildType)
    assertEquals("2026-01-06T00:00:00Z", info.buildTime)
    assertTrue(info.isDebug)
  }
}
