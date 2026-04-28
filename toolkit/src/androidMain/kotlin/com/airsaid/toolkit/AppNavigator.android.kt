package com.airsaid.toolkit

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Android implementation of [AppNavigator].
 */
actual object AppNavigator {

  private var applicationContext: Context? = null

  /**
   * Must be initialized with an Android [Context] before opening settings pages.
   */
  internal fun initialize(context: Context) {
    applicationContext = context.applicationContext
  }

  /**
   * Opens the system settings page on Android.
   */
  actual fun navigateToSystemSettings(): Boolean {
    val context = requireContext()
    return startActivitySafely(context, buildSystemSettingsIntent())
  }

  /**
   * Opens the app details page on Android.
   */
  actual fun navigateToAppDetails(): Boolean {
    val context = requireContext()
    return startActivitySafely(context, buildAppDetailsIntent(context.packageName))
  }

  /**
   * Opens the provided URL on Android.
   */
  actual fun navigateToUrl(url: String): Boolean {
    val context = requireContext()
    return startActivitySafely(context, buildUrlIntent(url))
  }

  /**
   * Opens the current app notification settings page on Android.
   */
  actual fun navigateToNotificationSettings(): Boolean {
    val context = requireContext()
    val intent = buildNotificationSettingsIntent(context)
      ?: buildAppDetailsIntent(context.packageName)
    return startActivitySafely(context, intent)
  }

  /**
   * Opens the email composer on Android.
   */
  actual fun navigateToEmail(to: String, subject: String?, body: String?): Boolean {
    val context = requireContext()
    return startActivitySafely(context, buildEmailIntent(to, subject, body))
  }

  /**
   * Opens the system dialer on Android.
   */
  actual fun navigateToDial(phone: String): Boolean {
    val context = requireContext()
    return startActivitySafely(context, buildDialIntent(phone))
  }

  /**
   * Opens the system SMS composer on Android.
   */
  actual fun navigateToSms(phone: String, body: String?): Boolean {
    val context = requireContext()
    return startActivitySafely(context, buildSmsIntent(phone, body))
  }

  /**
   * Opens the app store details page on Android.
   */
  actual fun navigateToAppStoreDetails(appId: String): Boolean {
    val context = requireContext()
    val marketIntent = buildPlayStoreDetailsIntent(appId)
    return if (startActivitySafely(context, marketIntent)) {
      true
    } else {
      startActivitySafely(context, buildPlayStoreWebIntent(appId))
    }
  }

  private fun requireContext(): Context {
    return applicationContext
      ?: throw IllegalStateException(
        "AppNavigator must be initialized with Context on Android. " +
            "Call Toolkit.initialize(ToolkitInitializer(context)) first."
      )
  }
}

internal fun buildSystemSettingsIntent(): Intent {
  return Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildAppDetailsIntent(packageName: String): Intent {
  return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    .setData(Uri.parse(buildAppDetailsUriString(packageName)))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildUrlIntent(url: String): Intent {
  return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildNotificationSettingsIntent(context: Context): Intent? {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
      .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  } else {
    null
  }
}

internal fun buildEmailIntent(to: String, subject: String?, body: String?): Intent {
  return Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("mailto:$to")
    if (!subject.isNullOrEmpty()) {
      putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    if (!body.isNullOrEmpty()) {
      putExtra(Intent.EXTRA_TEXT, body)
    }
  }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildDialIntent(phone: String): Intent {
  return Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildSmsIntent(phone: String, body: String?): Intent {
  return Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("smsto:$phone")
    if (!body.isNullOrEmpty()) {
      putExtra("sms_body", body)
    }
  }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildPlayStoreDetailsIntent(appId: String): Intent {
  return Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId"))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

internal fun buildPlayStoreWebIntent(appId: String): Intent {
  return Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appId"))
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

private fun startActivitySafely(context: Context, intent: Intent): Boolean {
  return try {
    context.startActivity(intent)
    true
  } catch (_: ActivityNotFoundException) {
    false
  } catch (_: SecurityException) {
    false
  }
}
