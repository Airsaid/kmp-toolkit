package com.airsaid.toolkit

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

internal actual object AppNavigatorFactory {

  private var applicationContext: Context? = null

  /**
   * Must be initialized with an Android [Context] before opening platform destinations.
   */
  internal fun initialize(context: Context) {
    applicationContext = context.applicationContext
  }

  actual fun create(): AppNavigator {
    return AndroidAppNavigator(requireContext())
  }

  private fun requireContext(): Context {
    return applicationContext
      ?: throw IllegalStateException(
        "AppNavigatorFactory must be initialized with Context on Android. " +
            "Call Toolkit.initialize(context) first."
      )
  }
}

private class AndroidAppNavigator(
  private val context: Context,
) : AppNavigator {

  override fun openSystemSettings(): AppNavigationResult {
    return context.startActivitySafely(
      intent = buildSystemSettingsIntent(),
      destination = AppNavigationDestination.SYSTEM_SETTINGS,
    )
  }

  override fun openAppSettings(): AppNavigationResult {
    return context.startActivitySafely(
      intent = buildAppSettingsIntent(context.packageName),
      destination = AppNavigationDestination.APP_SETTINGS,
    )
  }

  override fun openNotificationSettings(): AppNavigationResult {
    val destination = notificationSettingsDestinationFor(Build.VERSION.SDK_INT)
    val intent = buildNotificationSettingsIntent(context.packageName)
      ?: buildAppSettingsIntent(context.packageName)
    return context.startActivitySafely(intent = intent, destination = destination)
  }

  override fun openUrl(url: String): AppNavigationResult {
    val intent = buildUrlIntent(url) ?: return invalidInputResult()
    return context.startActivitySafely(
      intent = intent,
      destination = AppNavigationDestination.URL,
    )
  }

  override fun openEmail(request: EmailNavigationRequest): AppNavigationResult {
    val normalizedRequest = request.normalizedOrNull() ?: return invalidInputResult()
    return context.startActivitySafely(
      intent = buildEmailIntent(normalizedRequest),
      destination = AppNavigationDestination.EMAIL,
    )
  }

  override fun openDial(phoneNumber: String): AppNavigationResult {
    val normalizedPhoneNumber = phoneNumber.trimToNull() ?: return invalidInputResult()
    return context.startActivitySafely(
      intent = buildDialIntent(normalizedPhoneNumber),
      destination = AppNavigationDestination.DIAL,
    )
  }

  override fun openSms(request: SmsNavigationRequest): AppNavigationResult {
    val normalizedRequest = request.normalizedOrNull() ?: return invalidInputResult()
    return context.startActivitySafely(
      intent = buildSmsIntent(normalizedRequest),
      destination = AppNavigationDestination.SMS,
    )
  }

  override fun openAppStoreDetails(request: AppStoreDetailsNavigationRequest): AppNavigationResult {
    val packageName = request.normalizedAndroidPackageName() ?: return invalidInputResult()
    val marketResult = context.startActivitySafely(
      intent = buildPlayStoreDetailsIntent(packageName),
      destination = AppNavigationDestination.APP_STORE_DETAILS,
    )
    return if (marketResult.isPresented) {
      marketResult
    } else {
      context.startActivitySafely(
        intent = buildPlayStoreWebIntent(packageName),
        destination = AppNavigationDestination.APP_STORE_DETAILS,
      )
    }
  }
}

internal fun buildSystemSettingsIntent(): Intent {
  return Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildAppSettingsIntent(packageName: String): Intent {
  return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    .setData(Uri.parse(buildPackageUriString(packageName)))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildPackageUriString(packageName: String): String {
  return "package:${encodeUriComponent(packageName, allowed = URI_OPAQUE_ALLOWED)}"
}

internal fun buildUrlIntent(url: String): Intent? {
  val uri = url.trimToNull()?.let(Uri::parse) ?: return null
  if (uri.scheme.isNullOrBlank()) {
    return null
  }
  return Intent(Intent.ACTION_VIEW, uri)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildNotificationSettingsIntent(
  packageName: String,
  sdkInt: Int = Build.VERSION.SDK_INT,
): Intent? {
  return if (sdkInt >= Build.VERSION_CODES.O) {
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
      .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  } else {
    null
  }
}

internal fun notificationSettingsDestinationFor(sdkInt: Int): AppNavigationDestination {
  return if (sdkInt >= Build.VERSION_CODES.O) {
    AppNavigationDestination.NOTIFICATION_SETTINGS
  } else {
    AppNavigationDestination.APP_SETTINGS
  }
}

internal fun buildEmailIntent(request: EmailNavigationRequest): Intent {
  return Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse(buildMailToUriString(request.recipients))
    putExtra(Intent.EXTRA_EMAIL, request.recipients.toTypedArray())
    request.subject?.let { subject -> putExtra(Intent.EXTRA_SUBJECT, subject) }
    request.body?.let { body -> putExtra(Intent.EXTRA_TEXT, body) }
  }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildDialIntent(phoneNumber: String): Intent {
  return Intent(Intent.ACTION_DIAL, Uri.parse(buildTelUriString(phoneNumber)))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildSmsIntent(request: SmsNavigationRequest): Intent {
  return Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse(buildSmsUriString(request.phoneNumber))
    request.body?.let { body -> putExtra("sms_body", body) }
  }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildPlayStoreDetailsIntent(packageName: String): Intent {
  return Intent(Intent.ACTION_VIEW, Uri.parse(buildPlayStoreDetailsUriString(packageName)))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildPlayStoreWebIntent(packageName: String): Intent {
  return Intent(Intent.ACTION_VIEW, Uri.parse(buildPlayStoreWebUriString(packageName)))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildMailToUriString(recipients: List<String>): String {
  val encodedRecipients = recipients.joinToString(",") { recipient ->
    encodeUriComponent(recipient, allowed = MAILTO_RECIPIENT_ALLOWED)
  }
  return "mailto:$encodedRecipients"
}

internal fun buildTelUriString(phoneNumber: String): String {
  return "tel:${encodeUriComponent(phoneNumber, allowed = PHONE_ALLOWED)}"
}

internal fun buildSmsUriString(phoneNumber: String): String {
  return "smsto:${encodeUriComponent(phoneNumber, allowed = PHONE_ALLOWED)}"
}

internal fun buildPlayStoreDetailsUriString(packageName: String): String {
  return "market://details?id=${encodeUriComponent(packageName)}"
}

internal fun buildPlayStoreWebUriString(packageName: String): String {
  return "https://play.google.com/store/apps/details?id=${encodeUriComponent(packageName)}"
}

private const val URI_UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
private const val URI_OPAQUE_ALLOWED = "$URI_UNRESERVED."
private const val MAILTO_RECIPIENT_ALLOWED = "$URI_UNRESERVED@+,"
private const val PHONE_ALLOWED = "$URI_UNRESERVED+()-."

private fun encodeUriComponent(
  value: String,
  allowed: String = URI_UNRESERVED,
): String {
  val builder = StringBuilder()
  value.forEach { char ->
    if (char.code < 128 && allowed.contains(char)) {
      builder.append(char)
    } else {
      char.toString().encodeToByteArray().forEach { byte ->
        val valueByte = byte.toInt() and 0xFF
        builder.append('%')
        builder.append(valueByte.toString(16).uppercase().padStart(2, '0'))
      }
    }
  }
  return builder.toString()
}

private fun Context.startActivitySafely(
  intent: Intent,
  destination: AppNavigationDestination,
): AppNavigationResult {
  return try {
    startActivity(intent)
    AppNavigationResult.Presented(destination)
  } catch (error: ActivityNotFoundException) {
    AppNavigationResult.Failed(AppNavigationFailureReason.NO_TARGET, error)
  } catch (error: SecurityException) {
    AppNavigationResult.Failed(AppNavigationFailureReason.SECURITY_DENIED, error)
  } catch (error: RuntimeException) {
    AppNavigationResult.Failed(AppNavigationFailureReason.PRESENTATION_FAILED, error)
  }
}
