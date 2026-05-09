package com.airsaid.toolkit

import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

internal actual object AppNavigatorFactory {

  actual fun create(): AppNavigator {
    return IosAppNavigator
  }
}

private object IosAppNavigator : AppNavigator {

  override fun openSystemSettings(): AppNavigationResult {
    return AppNavigationResult.Failed(AppNavigationFailureReason.UNSUPPORTED_DESTINATION)
  }

  override fun openAppSettings(): AppNavigationResult {
    return openAppSettingsInternal()
  }

  override fun openNotificationSettings(): AppNavigationResult {
    return openAppSettingsInternal()
  }

  override fun openUrl(url: String): AppNavigationResult {
    val normalizedUrl = url.trimToNull() ?: return invalidInputResult()
    return openUrlString(normalizedUrl, AppNavigationDestination.URL)
  }

  override fun openEmail(request: EmailNavigationRequest): AppNavigationResult {
    val normalizedRequest = request.normalizedOrNull() ?: return invalidInputResult()
    return openUrlString(
      url = buildMailToUrlString(normalizedRequest),
      destination = AppNavigationDestination.EMAIL,
    )
  }

  override fun openDial(phoneNumber: String): AppNavigationResult {
    val normalizedPhoneNumber = phoneNumber.trimToNull() ?: return invalidInputResult()
    return openUrlString(
      url = "tel:${encodeUrlPath(normalizedPhoneNumber)}",
      destination = AppNavigationDestination.DIAL,
    )
  }

  override fun openSms(request: SmsNavigationRequest): AppNavigationResult {
    val normalizedRequest = request.normalizedOrNull() ?: return invalidInputResult()
    return openUrlString(
      url = buildSmsUrlString(normalizedRequest),
      destination = AppNavigationDestination.SMS,
    )
  }

  override fun openAppStoreDetails(request: AppStoreDetailsNavigationRequest): AppNavigationResult {
    val appId = request.normalizedIosAppId() ?: return invalidInputResult()
    return openUrlString(
      url = buildAppStoreDetailsUrlString(appId),
      destination = AppNavigationDestination.APP_STORE_DETAILS,
    )
  }
}

private fun openAppSettingsInternal(): AppNavigationResult {
  val url = NSURL(string = UIApplicationOpenSettingsURLString)
    ?: return AppNavigationResult.Failed(AppNavigationFailureReason.PRESENTATION_FAILED)
  return openUrl(url, AppNavigationDestination.APP_SETTINGS)
}

private fun openUrlString(
  url: String,
  destination: AppNavigationDestination,
): AppNavigationResult {
  val targetUrl = NSURL(string = url) ?: return invalidInputResult()
  if (targetUrl.scheme == null) {
    return invalidInputResult()
  }
  return openUrl(targetUrl, destination)
}

private fun openUrl(
  url: NSURL,
  destination: AppNavigationDestination,
): AppNavigationResult {
  val application = UIApplication.sharedApplication
  return if (application.canOpenURL(url)) {
    application.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    AppNavigationResult.Presented(destination)
  } else {
    AppNavigationResult.Failed(AppNavigationFailureReason.NO_TARGET)
  }
}

private fun buildMailToUrlString(request: EmailNavigationRequest): String {
  val recipients = request.recipients.joinToString(",") { recipient -> encodeUrlPath(recipient) }
  val queryItems = mutableListOf<String>()
  request.subject?.let { subject -> queryItems.add("subject=${encodeUrlQuery(subject)}") }
  request.body?.let { body -> queryItems.add("body=${encodeUrlQuery(body)}") }
  val query = if (queryItems.isEmpty()) "" else "?${queryItems.joinToString("&")}"
  return "mailto:$recipients$query"
}

private fun buildSmsUrlString(request: SmsNavigationRequest): String {
  val phoneNumber = encodeUrlPath(request.phoneNumber)
  return request.body?.let { body ->
    "sms:$phoneNumber?body=${encodeUrlQuery(body)}"
  } ?: "sms:$phoneNumber"
}

private fun buildAppStoreDetailsUrlString(appId: String): String {
  return "itms-apps://apps.apple.com/app/id${encodeUrlPath(appId)}"
}

private fun encodeUrlPath(value: String): String {
  val allowed = NSCharacterSet.characterSetWithCharactersInString(
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~@+,"
  )
  return (value as NSString).stringByAddingPercentEncodingWithAllowedCharacters(allowed) ?: value
}

private fun encodeUrlQuery(value: String): String {
  val allowed = NSCharacterSet.characterSetWithCharactersInString(
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
  )
  return (value as NSString).stringByAddingPercentEncodingWithAllowedCharacters(allowed) ?: value
}
