package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SensorOptionsTest {

  @Test
  fun shouldUseDefaultOptions() {
    val options = SensorOptions()

    assertNull(options.samplingRateHz)
    assertEquals(SensorDelay.NORMAL, options.delay)
    assertEquals(0L, options.maxReportLatencyMillis)
  }

  @Test
  fun shouldRejectInvalidSamplingRate() {
    assertFailsWith<IllegalArgumentException> {
      SensorOptions(samplingRateHz = 0)
    }
    assertFailsWith<IllegalArgumentException> {
      SensorOptions(samplingRateHz = -1)
    }
  }

  @Test
  fun shouldRejectInvalidReportLatency() {
    assertFailsWith<IllegalArgumentException> {
      SensorOptions(maxReportLatencyMillis = -1)
    }
  }
}
