package com.airsaid.toolkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppNavigatorTest {

  @Test
  fun presentedResultIsPresented() {
    val result = AppNavigationResult.Presented(AppNavigationDestination.URL)

    assertTrue(result.isPresented)
    assertEquals(AppNavigationDestination.URL, result.destination)
  }

  @Test
  fun failedResultIsNotPresented() {
    val result = AppNavigationResult.Failed(AppNavigationFailureReason.NO_TARGET)

    assertFalse(result.isPresented)
    assertEquals(AppNavigationFailureReason.NO_TARGET, result.reason)
  }

  @Test
  fun emailRequestRequiresAtLeastOneNonBlankRecipient() {
    assertNull(EmailNavigationRequest(emptyList()).normalizedOrNull())
    assertNull(EmailNavigationRequest(listOf("support@example.com", " ")).normalizedOrNull())
  }

  @Test
  fun emailRequestTrimsRecipientsAndIgnoresBlankSubjectAndBody() {
    val request = EmailNavigationRequest(
      recipients = listOf(" support@example.com "),
      subject = " ",
      body = "\n",
    ).normalizedOrNull()

    assertEquals(listOf("support@example.com"), request?.recipients)
    assertNull(request?.subject)
    assertNull(request?.body)
  }

  @Test
  fun smsRequestRequiresNonBlankPhoneNumber() {
    assertNull(SmsNavigationRequest(phoneNumber = " ").normalizedOrNull())
  }

  @Test
  fun smsRequestTrimsPhoneNumberAndIgnoresBlankBody() {
    val request = SmsNavigationRequest(
      phoneNumber = " 10086 ",
      body = " ",
    ).normalizedOrNull()

    assertEquals("10086", request?.phoneNumber)
    assertNull(request?.body)
  }

  @Test
  fun appStoreRequestTrimsPlatformIds() {
    val request = AppStoreDetailsNavigationRequest(
      androidPackageName = " com.example.app ",
      iosAppId = " 123456789 ",
    )

    assertEquals("com.example.app", request.normalizedAndroidPackageName())
    assertEquals("123456789", request.normalizedIosAppId())
  }

  @Test
  fun appStoreRequestIgnoresBlankPlatformIds() {
    val request = AppStoreDetailsNavigationRequest(
      androidPackageName = " ",
      iosAppId = "\n",
    )

    assertNull(request.normalizedAndroidPackageName())
    assertNull(request.normalizedIosAppId())
  }
}
