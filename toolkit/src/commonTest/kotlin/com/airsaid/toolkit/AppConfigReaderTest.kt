package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppConfigReaderTest {

  @Test
  fun appConfigParsesValidStringValues() {
    assertEquals(true, parseAppConfigBoolean("true", defaultValue = false))
    assertEquals(42, parseAppConfigInt("42", defaultValue = 7))
    assertEquals(42L, parseAppConfigLong("42", defaultValue = 7L))
    assertEquals(1.5f, parseAppConfigFloat("1.5", defaultValue = 2f))
  }

  @Test
  fun appConfigUsesDefaultsForInvalidStringValues() {
    assertEquals(true, parseAppConfigBoolean("yes", defaultValue = true))
    assertEquals(7, parseAppConfigInt("many", defaultValue = 7))
    assertEquals(7L, parseAppConfigLong("many", defaultValue = 7L))
    assertEquals(2f, parseAppConfigFloat("many", defaultValue = 2f))
  }

  @Test
  fun appConfigKeepsNullWhenInvalidStringHasNoDefault() {
    assertNull(parseAppConfigBoolean("yes", defaultValue = null))
    assertNull(parseAppConfigInt("many", defaultValue = null))
    assertNull(parseAppConfigLong("many", defaultValue = null))
    assertNull(parseAppConfigFloat("many", defaultValue = null))
  }

  @Test
  fun appConfigParsesNumberValues() {
    assertEquals(42, parseAppConfigInt(42, defaultValue = 7))
    assertEquals(42L, parseAppConfigLong(42, defaultValue = 7L))
    assertEquals(42f, parseAppConfigFloat(42, defaultValue = 7f))
  }
}
