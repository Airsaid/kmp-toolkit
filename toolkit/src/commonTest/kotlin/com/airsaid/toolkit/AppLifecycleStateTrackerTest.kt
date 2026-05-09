package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLifecycleStateTrackerTest {

  @Test
  fun coldStartIsReportedOnFirstForegroundEntry() {
    val tracker = AppLifecycleStateTracker()

    val update = tracker.update(isInForeground = true, isVisible = true)
    val status = update.status

    assertTrue(status.isInForeground)
    assertTrue(status.isVisible)
    assertEquals(AppStartType.COLD, status.lastStartType)
    assertEquals(AppStartType.COLD, update.startType)
  }

  @Test
  fun hotStartIsReportedWhenReturningToForeground() {
    val tracker = AppLifecycleStateTracker()

    tracker.update(isInForeground = true, isVisible = true)
    tracker.update(isInForeground = false, isVisible = false)
    val update = tracker.update(isInForeground = true, isVisible = true)
    val status = update.status

    assertEquals(AppStartType.HOT, status.lastStartType)
    assertEquals(AppStartType.HOT, update.startType)
  }

  @Test
  fun visibilityChangesDoNotEmitStartEvents() {
    val tracker = AppLifecycleStateTracker()

    tracker.update(isInForeground = true, isVisible = true)
    val update = tracker.update(isInForeground = true, isVisible = false)
    val status = update.status

    assertEquals(AppStartType.COLD, status.lastStartType)
    assertEquals(null, update.startType)
  }
}
