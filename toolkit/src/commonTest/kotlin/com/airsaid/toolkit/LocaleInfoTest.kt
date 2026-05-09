package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocaleInfoTest {

  @Test
  fun buildLocaleInfoNormalizesLanguageAndRegion() {
    val info = buildLocaleInfo("EN", "us")
    assertEquals("en-US", info.languageTag)
    assertEquals("en", info.languageCode)
    assertNull(info.scriptCode)
    assertEquals("US", info.regionCode)
    assertNull(info.variant)
  }

  @Test
  fun buildLocaleInfoUsesUndWhenMissingLanguage() {
    val info = buildLocaleInfo(null, null)
    assertEquals("und", info.languageTag)
    assertEquals("und", info.languageCode)
    assertNull(info.regionCode)
  }

  @Test
  fun buildLocaleInfoFromTagPreservesScriptAndRegion() {
    val info = buildLocaleInfoFromTag("zh-Hant-TW")
    assertEquals("zh-Hant-TW", info.languageTag)
    assertEquals("zh", info.languageCode)
    assertEquals("Hant", info.scriptCode)
    assertEquals("TW", info.regionCode)
    assertNull(info.variant)
  }

  @Test
  fun buildLocaleInfoFromTagParsesCyrillicSerbian() {
    val info = buildLocaleInfoFromTag("sr-Cyrl-RS")
    assertEquals("sr-Cyrl-RS", info.languageTag)
    assertEquals("sr", info.languageCode)
    assertEquals("Cyrl", info.scriptCode)
    assertEquals("RS", info.regionCode)
  }

  @Test
  fun buildLocaleInfoFromTagAcceptsNumericRegion() {
    val info = buildLocaleInfoFromTag("es-419")
    assertEquals("es-419", info.languageTag)
    assertEquals("es", info.languageCode)
    assertNull(info.scriptCode)
    assertEquals("419", info.regionCode)
  }

  @Test
  fun buildLocaleInfoFromTagHandlesLanguageRegion() {
    val info = buildLocaleInfoFromTag("en-US")
    assertEquals("en-US", info.languageTag)
    assertEquals("en", info.languageCode)
    assertEquals("US", info.regionCode)
  }

  @Test
  fun buildLocaleInfoFromTagHandlesUnderscore() {
    val info = buildLocaleInfoFromTag("en_US")
    assertEquals("en-US", info.languageTag)
    assertEquals("en", info.languageCode)
    assertEquals("US", info.regionCode)
  }

  @Test
  fun buildLocaleInfoFromTagIgnoresFoundationIdentifierExtensions() {
    val info = buildLocaleInfoFromTag("en_US@calendar=gregorian")
    assertEquals("en-US", info.languageTag)
    assertEquals("en", info.languageCode)
    assertEquals("US", info.regionCode)
  }

  @Test
  fun buildLocaleInfoFromTagKeepsVariantSubtags() {
    val info = buildLocaleInfoFromTag("sl-rozaj-biske")
    assertEquals("sl-rozaj-biske", info.languageTag)
    assertEquals("sl", info.languageCode)
    assertEquals("rozaj-biske", info.variant)
  }

  @Test
  fun buildLocaleInfoFromTagUsesUndWhenBlank() {
    val info = buildLocaleInfoFromTag("  ")
    assertEquals("und", info.languageTag)
    assertEquals("und", info.languageCode)
    assertNull(info.regionCode)
  }

  @Test
  fun buildLocaleInfoFromTagUsesUndWhenLanguageIsUnknown() {
    val info = buildLocaleInfoFromTag("123-US")
    assertEquals("und-US", info.languageTag)
    assertEquals("und", info.languageCode)
    assertEquals("US", info.regionCode)
  }
}
