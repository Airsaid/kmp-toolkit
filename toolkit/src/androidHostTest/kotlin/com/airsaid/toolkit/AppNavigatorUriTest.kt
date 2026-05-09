package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavigatorUriTest {

  @Test
  fun appSettingsUriUsesPackageScheme() {
    assertEquals("package:com.example.app", buildPackageUriString("com.example.app"))
  }

  @Test
  fun notificationDestinationFallsBackToAppSettingsBeforeApi26() {
    assertEquals(AppNavigationDestination.APP_SETTINGS, notificationSettingsDestinationFor(25))
    assertEquals(AppNavigationDestination.NOTIFICATION_SETTINGS, notificationSettingsDestinationFor(26))
  }

  @Test
  fun mailToUriEncodesRecipientsWithoutAddingQuery() {
    val uri = buildMailToUriString(
      listOf(
        "support@example.com",
        "team+test@example.com",
      )
    )

    assertEquals("mailto:support@example.com,team+test@example.com", uri)
  }

  @Test
  fun dialUriEncodesCharactersThatWouldBreakOpaqueUri() {
    val uri = buildTelUriString("+1 555?0100#1")

    assertEquals("tel:+1%20555%3F0100%231", uri)
  }

  @Test
  fun smsUriEncodesCharactersThatWouldBreakOpaqueUri() {
    val uri = buildSmsUriString("+1 555?0100#1")

    assertEquals("smsto:+1%20555%3F0100%231", uri)
  }

  @Test
  fun playStoreUrisEncodePackageNameAsQueryParameter() {
    val packageName = "com.example.app?channel=a&b#c"

    assertEquals(
      "market://details?id=com.example.app%3Fchannel%3Da%26b%23c",
      buildPlayStoreDetailsUriString(packageName),
    )
    assertEquals(
      "https://play.google.com/store/apps/details?id=com.example.app%3Fchannel%3Da%26b%23c",
      buildPlayStoreWebUriString(packageName),
    )
  }
}
