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

  @Test
  fun resolveKeyboardStatusReturnsHiddenWhenImeIsNotVisible() {
    val status = resolveKeyboardStatus(isVisible = false, heightPx = 200, thresholdPx = 100)

    assertFalse(status.isVisible)
    assertTrue(status.heightPx == 0)
  }

  @Test
  fun resolveKeyboardStatusReturnsHiddenWhenHeightDoesNotExceedThreshold() {
    val status = resolveKeyboardStatus(isVisible = true, heightPx = 100, thresholdPx = 100)

    assertFalse(status.isVisible)
    assertTrue(status.heightPx == 0)
  }

  @Test
  fun resolveKeyboardStatusReturnsVisibleHeightWhenImeIsVisibleAboveThreshold() {
    val status = resolveKeyboardStatus(isVisible = true, heightPx = 101, thresholdPx = 100)

    assertTrue(status.isVisible)
    assertTrue(status.heightPx == 101)
  }
}
