package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavigatorTest {

  @Test
  fun buildAppDetailsUriStringUsesPackageScheme() {
    val uri = buildAppDetailsUriString("com.example.app")
    assertEquals("package:com.example.app", uri)
  }
}
