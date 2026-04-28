package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShareOptionsTest {

  @Test
  fun defaultsAreEmpty() {
    val options = ShareOptions()

    assertEquals(null, options.title)
    assertTrue(options.excludedActivities.isEmpty())
  }
}
