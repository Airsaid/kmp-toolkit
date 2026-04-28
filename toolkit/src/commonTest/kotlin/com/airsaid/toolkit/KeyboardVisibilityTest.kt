package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyboardVisibilityTest {

  @Test
  fun resolveKeyboardVisibilityUsesThreshold() {
    assertFalse(resolveKeyboardVisibility(heightPx = 100, thresholdPx = 100))
    assertTrue(resolveKeyboardVisibility(heightPx = 101, thresholdPx = 100))
  }
}
