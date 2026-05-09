package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("DEPRECATION")
class AppLifecycleStateTrackerTest {

  @Test
  fun coldStartIsCountedOnFirstForegroundEntry() {
    val tracker = AppLifecycleStateTracker()

    val update = tracker.update(isInForeground = true, isVisible = true)
    val status = update.status

    assertTrue(status.isInForeground)
    assertTrue(status.isVisible)
    assertTrue(status.isFirstLaunch)
    assertEquals(1, status.coldStartCount)
    assertEquals(0, status.hotStartCount)
    assertEquals(AppStartType.COLD, status.lastStartType)
    assertEquals(AppStartType.COLD, update.startType)

    tracker.clearFirstLaunchFlag()

    assertFalse(tracker.currentStatus.isFirstLaunch)
  }

  @Test
  fun hotStartIsCountedWhenReturningToForeground() {
    val tracker = AppLifecycleStateTracker()

    tracker.update(isInForeground = true, isVisible = true)
    tracker.clearFirstLaunchFlag()
    tracker.update(isInForeground = false, isVisible = false)
    val update = tracker.update(isInForeground = true, isVisible = true)
    val status = update.status

    assertFalse(status.isFirstLaunch)
    assertEquals(1, status.coldStartCount)
    assertEquals(1, status.hotStartCount)
    assertEquals(AppStartType.HOT, status.lastStartType)
    assertEquals(AppStartType.HOT, update.startType)
  }

  @Test
  fun visibilityChangesDoNotAffectStartCounts() {
    val tracker = AppLifecycleStateTracker()

    tracker.update(isInForeground = true, isVisible = true)
    tracker.clearFirstLaunchFlag()
    val update = tracker.update(isInForeground = true, isVisible = false)
    val status = update.status

    assertEquals(1, status.coldStartCount)
    assertEquals(0, status.hotStartCount)
    assertEquals(AppStartType.COLD, status.lastStartType)
    assertEquals(null, update.startType)
  }
}
