package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertNull

class DeviceInfoTest {

  @Test
  fun systemVersionCodeCanBeUnavailable() {
    val deviceInfo = DeviceInfo(
      deviceModel = "iPhone",
      systemName = "iOS",
      systemVersion = "18.0",
      systemVersionCode = null,
      manufacturer = ManufacturerInfo(
        manufacturer = "Apple",
        brand = "Apple",
      ),
      deviceType = DeviceTypeInfo(
        isTablet = false,
        isEmulator = false,
      ),
      window = null,
      screen = DisplayInfo(
        widthPx = 1179,
        heightPx = 2556,
        widthLogical = 393,
        heightLogical = 852,
        density = 3f,
        densityDpi = 480,
        isLandscape = false,
      ),
      timeZone = TimeZoneInfo(
        id = "Asia/Shanghai",
        offsetMinutes = 480,
      ),
      locale = LocaleBundle(
        current = buildLocaleInfoFromTag("en-US"),
        preferred = listOf(buildLocaleInfoFromTag("en-US")),
      ),
    )

    assertNull(deviceInfo.systemVersionCode)
    assertNull(deviceInfo.window)
  }
}
