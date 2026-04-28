package com.airsaid.toolkit

/**
 * Handles app-level navigation actions when supported by the platform.
 *
 * Android requires [Toolkit.initialize] before use.
 */
expect object AppNavigator {

  /**
   * Opens the system settings page if supported.
   *
   * @return True if the request was handled by the system.
   */
  fun navigateToSystemSettings(): Boolean

  /**
   * Opens the current app details/settings page if supported.
   *
   * @return True if the request was handled by the system.
   */
  fun navigateToAppDetails(): Boolean

  /**
   * Opens the current app notification settings page if supported.
   *
   * @return True if the request was handled by the system.
   */
  fun navigateToNotificationSettings(): Boolean

  /**
   * Opens the provided URL using the system handler if supported.
   *
   * @return True if the request was handled by the system.
   */
  fun navigateToUrl(url: String): Boolean

  /**
   * Opens the email composer using the system handler if supported.
   *
   * @return True if the request was handled by the system.
   */
  fun navigateToEmail(to: String, subject: String? = null, body: String? = null): Boolean

  /**
   * Opens the system dialer if supported.
   *
   * @return True if the request was handled by the system.
   */
  fun navigateToDial(phone: String): Boolean

  /**
   * Opens the system SMS composer if supported.
   *
   * @return True if the request was handled by the system.
   */
  fun navigateToSms(phone: String, body: String? = null): Boolean

  /**
   * Opens the app store details page for the provided app id if supported.
   *
   * @return True if the request was handled by the system.
   */
  fun navigateToAppStoreDetails(appId: String): Boolean
}

internal fun buildAppDetailsUriString(packageName: String): String {
  return "package:$packageName"
}
