package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleInfoTest {

  @Test
  fun tagUsesLanguageOnlyWhenRegionMissing() {
    val info = LocaleInfo(languageCode = "en", regionCode = "")
    assertEquals("en", info.tag)
  }

  @Test
  fun tagIncludesRegionWhenProvided() {
    val info = LocaleInfo(languageCode = "zh", regionCode = "CN")
    assertEquals("zh-CN", info.tag)
  }

  @Test
  fun buildLocaleInfoNormalizesValues() {
    val info = buildLocaleInfo("EN", "us")
    assertEquals("en", info.languageCode)
    assertEquals("US", info.regionCode)
    assertEquals("en-US", info.tag)
  }

  @Test
  fun buildLocaleInfoUsesUndWhenMissingLanguage() {
    val info = buildLocaleInfo(null, null)
    assertEquals("und", info.languageCode)
    assertEquals("", info.regionCode)
    assertEquals("und", info.tag)
  }

  @Test
  fun buildLocaleInfoFromTagParsesLanguageRegion() {
    val info = buildLocaleInfoFromTag("zh-CN")
    assertEquals("zh", info.languageCode)
    assertEquals("CN", info.regionCode)
    assertEquals("zh-CN", info.tag)
  }

  @Test
  fun buildLocaleInfoFromTagHandlesUnderscore() {
    val info = buildLocaleInfoFromTag("en_US")
    assertEquals("en", info.languageCode)
    assertEquals("US", info.regionCode)
    assertEquals("en-US", info.tag)
  }

  @Test
  fun buildLocaleInfoFromTagUsesLanguageOnly() {
    val info = buildLocaleInfoFromTag("fr")
    assertEquals("fr", info.languageCode)
    assertEquals("", info.regionCode)
    assertEquals("fr", info.tag)
  }

  @Test
  fun buildLocaleInfoFromTagAcceptsThreeLetterRegion() {
    val info = buildLocaleInfoFromTag("es-419")
    assertEquals("es", info.languageCode)
    assertEquals("419", info.regionCode)
    assertEquals("es-419", info.tag)
  }

  @Test
  fun buildLocaleInfoFromTagUsesUndWhenBlank() {
    val info = buildLocaleInfoFromTag("  ")
    assertEquals("und", info.languageCode)
    assertEquals("", info.regionCode)
    assertEquals("und", info.tag)
  }
}
