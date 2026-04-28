package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SensorOptionsTest {

  @Test
  fun shouldUseDefaultOptions() {
    val options = SensorOptions()

    assertNull(options.samplingRateHz)
    assertEquals(SensorDelay.NORMAL, options.delay)
    assertFalse(options.batchEnabled)
  }
}
