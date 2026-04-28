package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals

class HapticFeedbackTypeTest {

  @Test
  fun hapticFeedbackTypeHasExpectedOrder() {
    val values = HapticFeedbackType.values().toList()
    assertEquals(
      listOf(
        HapticFeedbackType.SUCCESS,
        HapticFeedbackType.WARNING,
        HapticFeedbackType.ERROR,
        HapticFeedbackType.SELECTION,
      ),
      values,
    )
  }
}
