package com.airsaid.toolkit

import platform.Foundation.NSURL
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/**
 * iOS implementation of [AppNavigator].
 */
actual object AppNavigator {

  /**
   * Opens the app settings page on iOS.
   */
  actual fun navigateToSystemSettings(): Boolean {
    return openAppSettingsInternal()
  }

  /**
   * Opens the app settings page on iOS.
   */
  actual fun navigateToAppDetails(): Boolean {
    return openAppSettingsInternal()
  }

  /**
   * Opens the provided URL on iOS.
   */
  actual fun navigateToUrl(url: String): Boolean {
    return openUrlString(url)
  }

  /**
   * Opens the current app notification settings page on iOS.
   */
  actual fun navigateToNotificationSettings(): Boolean {
    return openAppSettingsInternal()
  }

  /**
   * Opens the email composer on iOS.
   */
  actual fun navigateToEmail(to: String, subject: String?, body: String?): Boolean {
    val url = buildMailToUrlString(to, subject, body)
    return openUrlString(url)
  }

  /**
   * Opens the system dialer on iOS.
   */
  actual fun navigateToDial(phone: String): Boolean {
    return openUrlString("tel:$phone")
  }

  /**
   * Opens the system SMS composer on iOS.
   */
  actual fun navigateToSms(phone: String, body: String?): Boolean {
    val url = if (body.isNullOrEmpty()) {
      "sms:$phone"
    } else {
      "sms:$phone?body=${encodeUrlQuery(body)}"
    }
    return openUrlString(url)
  }

  /**
   * Opens the app store details page on iOS.
   */
  actual fun navigateToAppStoreDetails(appId: String): Boolean {
    return openUrlString(buildAppStoreDetailsUrlString(appId))
  }
}

private fun openAppSettingsInternal(): Boolean {
  val url = NSURL(string = UIApplicationOpenSettingsURLString) ?: return false
  return openUrl(url)
}

private fun openUrlString(url: String): Boolean {
  val targetUrl = NSURL(string = url) ?: return false
  return openUrl(targetUrl)
}

private fun openUrl(url: NSURL): Boolean {
  val application = UIApplication.sharedApplication
  return if (application.canOpenURL(url)) {
    application.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    true
  } else {
    false
  }
}

private fun buildMailToUrlString(to: String, subject: String?, body: String?): String {
  val queryItems = mutableListOf<String>()
  if (!subject.isNullOrEmpty()) {
    queryItems.add("subject=${encodeUrlQuery(subject)}")
  }
  if (!body.isNullOrEmpty()) {
    queryItems.add("body=${encodeUrlQuery(body)}")
  }
  val query = if (queryItems.isEmpty()) "" else "?${queryItems.joinToString("&")}"
  return "mailto:$to$query"
}

private fun buildAppStoreDetailsUrlString(appId: String): String {
  return "itms-apps://apps.apple.com/app/id$appId"
}

private fun encodeUrlQuery(value: String): String {
  val allowed = NSCharacterSet.characterSetWithCharactersInString(
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
  )
  return (value as NSString).stringByAddingPercentEncodingWithAllowedCharacters(allowed) ?: value
}
