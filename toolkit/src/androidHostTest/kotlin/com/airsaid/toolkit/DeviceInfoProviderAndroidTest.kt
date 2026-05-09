package com.airsaid.toolkit

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceInfoProviderAndroidTest {

  @Test
  fun localeConversionPreservesLanguageTag() {
    val locale = Locale.Builder()
      .setLanguage("zh")
      .setScript("Hant")
      .setRegion("TW")
      .build()

    val info = buildLocaleInfoFromTag(locale.toLanguageTag())

    assertEquals("zh-Hant-TW", info.languageTag)
    assertEquals("zh", info.languageCode)
    assertEquals("Hant", info.scriptCode)
    assertEquals("TW", info.regionCode)
  }

  @Test
  fun displayInfoMapsBoundsToLogicalSize() {
    val info = buildAndroidDisplayInfo(
      widthPx = 1200,
      heightPx = 800,
      density = 2f,
      densityDpi = 320,
    )

    assertEquals(1200, info.widthPx)
    assertEquals(800, info.heightPx)
    assertEquals(600, info.widthLogical)
    assertEquals(400, info.heightLogical)
    assertEquals(2f, info.density)
    assertEquals(320, info.densityDpi)
    assertTrue(info.isLandscape)
  }

  @Test
  fun displayInfoKeepsExplicitLogicalSizeForScreenSnapshot() {
    val info = buildAndroidDisplayInfo(
      widthPx = 1440,
      heightPx = 2960,
      density = 3f,
      densityDpi = 480,
      widthLogical = 411,
      heightLogical = 891,
    )

    assertEquals(1440, info.widthPx)
    assertEquals(2960, info.heightPx)
    assertEquals(411, info.widthLogical)
    assertEquals(891, info.heightLogical)
    assertFalse(info.isLandscape)
  }

  @Test
  fun currentWindowDisplayInfoIsNullWithoutActivity() {
    assertNull(resolveCurrentWindowDisplayInfo(null))
  }
}
