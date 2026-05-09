package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyboardVisibilityTest {

  @Test
  fun resolveKeyboardStatusReturnsHiddenWhenImeIsNotVisible() {
    val status = resolveKeyboardStatus(isVisible = false, heightPx = 200)

    assertFalse(status.isVisible)
    assertTrue(status.heightPx == 0)
  }

  @Test
  fun resolveKeyboardStatusReturnsHiddenWhenHeightIsZero() {
    val status = resolveKeyboardStatus(isVisible = true, heightPx = 0)

    assertFalse(status.isVisible)
    assertTrue(status.heightPx == 0)
  }

  @Test
  fun resolveKeyboardStatusReturnsVisibleHeightWhenImeIsVisible() {
    val status = resolveKeyboardStatus(isVisible = true, heightPx = 101)

    assertTrue(status.isVisible)
    assertTrue(status.heightPx == 101)
  }
}
