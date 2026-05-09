package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals

class SensorEventTest {

  @Test
  fun shouldCompareEventsBySensorValues() {
    val first = SensorEvent(
      type = SensorType.ACCELEROMETER,
      values = listOf(1f, 2f, 3f),
      timestampNanos = 100,
      accuracy = SensorAccuracy.HIGH,
    )
    val second = SensorEvent(
      type = SensorType.ACCELEROMETER,
      values = listOf(1f, 2f, 3f),
      timestampNanos = 100,
      accuracy = SensorAccuracy.HIGH,
    )

    assertEquals(first, second)
    assertEquals(first.hashCode(), second.hashCode())
  }

  @Test
  fun shouldStoreRequiredPermissionInAvailability() {
    val availability = SensorAvailability(
      isAvailable = false,
      reason = "Missing required permission.",
      requiredPermission = "android.permission.ACTIVITY_RECOGNITION",
    )

    assertEquals("android.permission.ACTIVITY_RECOGNITION", availability.requiredPermission)
  }
}
