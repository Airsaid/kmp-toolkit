package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLifecycleStateTrackerTest {

  @Test
  fun coldStartIsCountedOnFirstForegroundEntry() {
    val tracker = AppLifecycleStateTracker()

    val status = tracker.update(isInForeground = true, isVisible = true)

    assertTrue(status.isInForeground)
    assertTrue(status.isVisible)
    assertTrue(status.isFirstLaunch)
    assertEquals(1, status.coldStartCount)
    assertEquals(0, status.hotStartCount)
    assertEquals(AppStartType.COLD, status.lastStartType)
  }

  @Test
  fun hotStartIsCountedWhenReturningToForeground() {
    val tracker = AppLifecycleStateTracker()

    tracker.update(isInForeground = true, isVisible = true)
    tracker.update(isInForeground = false, isVisible = false)
    val status = tracker.update(isInForeground = true, isVisible = true)

    assertFalse(status.isFirstLaunch)
    assertEquals(1, status.coldStartCount)
    assertEquals(1, status.hotStartCount)
    assertEquals(AppStartType.HOT, status.lastStartType)
  }

  @Test
  fun visibilityChangesDoNotAffectStartCounts() {
    val tracker = AppLifecycleStateTracker()

    tracker.update(isInForeground = true, isVisible = true)
    val status = tracker.update(isInForeground = true, isVisible = false)

    assertEquals(1, status.coldStartCount)
    assertEquals(0, status.hotStartCount)
    assertEquals(AppStartType.COLD, status.lastStartType)
  }
}
