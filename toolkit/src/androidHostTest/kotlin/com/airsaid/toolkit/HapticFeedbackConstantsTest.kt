package com.airsaid.toolkit

import android.os.Build
import android.view.HapticFeedbackConstants
import kotlin.test.Test
import kotlin.test.assertEquals

class HapticFeedbackConstantsTest {

  @Test
  fun selectionUsesClockTickBeforeApi34() {
    assertEquals(
      HapticFeedbackConstants.CLOCK_TICK,
      hapticFeedbackConstantFor(HapticFeedbackType.SELECTION, sdkInt = 24),
    )
    assertEquals(
      HapticFeedbackConstants.CLOCK_TICK,
      hapticFeedbackConstantFor(HapticFeedbackType.SELECTION, sdkInt = 30),
    )
  }

  @Test
  fun selectionUsesSegmentTickOnApi34AndLater() {
    assertEquals(
      HapticFeedbackConstants.SEGMENT_TICK,
      hapticFeedbackConstantFor(HapticFeedbackType.SELECTION, sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE),
    )
    assertEquals(
      HapticFeedbackConstants.SEGMENT_TICK,
      hapticFeedbackConstantFor(HapticFeedbackType.SELECTION, sdkInt = 36),
    )
  }

  @Test
  fun successUsesContextClickBeforeApi30AndConfirmAfterwards() {
    assertEquals(
      HapticFeedbackConstants.CONTEXT_CLICK,
      hapticFeedbackConstantFor(HapticFeedbackType.SUCCESS, sdkInt = 24),
    )
    assertEquals(
      HapticFeedbackConstants.CONFIRM,
      hapticFeedbackConstantFor(HapticFeedbackType.SUCCESS, sdkInt = Build.VERSION_CODES.Q),
    )
  }

  @Test
  fun warningAndErrorUseLongPressBeforeApi30AndRejectAfterwards() {
    listOf(HapticFeedbackType.WARNING, HapticFeedbackType.ERROR).forEach { type ->
      assertEquals(HapticFeedbackConstants.LONG_PRESS, hapticFeedbackConstantFor(type, sdkInt = 24))
      assertEquals(HapticFeedbackConstants.REJECT, hapticFeedbackConstantFor(type, sdkInt = Build.VERSION_CODES.Q))
      assertEquals(HapticFeedbackConstants.REJECT, hapticFeedbackConstantFor(type, sdkInt = 36))
    }
  }
}
