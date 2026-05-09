package com.airsaid.toolkit

/**
 * Handles app-level navigation actions when supported by the platform.
 *
 * Android requires [Toolkit.initialize] before use.
 */
interface AppNavigator {

  /**
   * Opens the platform system settings page when supported.
   */
  fun openSystemSettings(): AppNavigationResult

  /**
   * Opens the current app settings page when supported.
   */
  fun openAppSettings(): AppNavigationResult

  /**
   * Opens the current app notification settings page when supported.
   */
  fun openNotificationSettings(): AppNavigationResult

  /**
   * Opens the provided URL using the system handler when supported.
   */
  fun openUrl(url: String): AppNavigationResult

  /**
   * Opens the email composer using the system handler when supported.
   */
  fun openEmail(request: EmailNavigationRequest): AppNavigationResult

  /**
   * Opens the email composer using the system handler when supported.
   */
  fun openEmail(
    to: String,
    subject: String? = null,
    body: String? = null,
  ): AppNavigationResult {
    return openEmail(
      EmailNavigationRequest(
        recipients = listOf(to),
        subject = subject,
        body = body,
      )
    )
  }

  /**
   * Opens the system dialer when supported.
   */
  fun openDial(phoneNumber: String): AppNavigationResult

  /**
   * Opens the system SMS composer when supported.
   */
  fun openSms(request: SmsNavigationRequest): AppNavigationResult

  /**
   * Opens the system SMS composer when supported.
   */
  fun openSms(phoneNumber: String, body: String? = null): AppNavigationResult {
    return openSms(SmsNavigationRequest(phoneNumber = phoneNumber, body = body))
  }

  /**
   * Opens the app store details page for the provided platform app id when supported.
   */
  fun openAppStoreDetails(request: AppStoreDetailsNavigationRequest): AppNavigationResult
}

/**
 * Result of an app-level navigation request.
 */
sealed interface AppNavigationResult {

  /**
   * True when the platform accepted the navigation request for presentation.
   */
  val isPresented: Boolean

  /**
   * The platform accepted the navigation request.
   */
  data class Presented(
    val destination: AppNavigationDestination,
  ) : AppNavigationResult {
    override val isPresented: Boolean = true
  }

  /**
   * The navigation request failed before it could be presented.
   */
  data class Failed(
    val reason: AppNavigationFailureReason,
    val cause: Throwable? = null,
  ) : AppNavigationResult {
    override val isPresented: Boolean = false
  }
}

/**
 * Destination that was accepted by the platform.
 */
enum class AppNavigationDestination {
  SYSTEM_SETTINGS,
  APP_SETTINGS,
  NOTIFICATION_SETTINGS,
  URL,
  EMAIL,
  DIAL,
  SMS,
  APP_STORE_DETAILS,
}

/**
 * Reasons an app-level navigation request can fail before presentation.
 */
enum class AppNavigationFailureReason {
  INVALID_INPUT,
  UNSUPPORTED_DESTINATION,
  NO_TARGET,
  SECURITY_DENIED,
  PRESENTATION_FAILED,
}

/**
 * Email composer request.
 *
 * @property recipients Email recipients. Must contain at least one non-blank value.
 * @property subject Optional subject. Blank values are ignored.
 * @property body Optional body. Blank values are ignored.
 */
data class EmailNavigationRequest(
  val recipients: List<String>,
  val subject: String? = null,
  val body: String? = null,
)

/**
 * SMS composer request.
 *
 * @property phoneNumber Destination phone number. Must be non-blank.
 * @property body Optional body. Blank values are ignored.
 */
data class SmsNavigationRequest(
  val phoneNumber: String,
  val body: String? = null,
)

/**
 * App store details request.
 *
 * @property androidPackageName Android application package name.
 * @property iosAppId App Store numeric application id.
 */
data class AppStoreDetailsNavigationRequest(
  val androidPackageName: String? = null,
  val iosAppId: String? = null,
)

internal expect object AppNavigatorFactory {

  /**
   * Creates a platform-specific [AppNavigator] instance.
   */
  fun create(): AppNavigator
}

internal fun EmailNavigationRequest.normalizedOrNull(): EmailNavigationRequest? {
  val normalizedRecipients = recipients.map { recipient -> recipient.trim() }
  if (normalizedRecipients.isEmpty() || normalizedRecipients.any { recipient -> recipient.isEmpty() }) {
    return null
  }
  return copy(
    recipients = normalizedRecipients,
    subject = subject.trimToNull(),
    body = body.trimToNull(),
  )
}

internal fun SmsNavigationRequest.normalizedOrNull(): SmsNavigationRequest? {
  val normalizedPhoneNumber = phoneNumber.trim()
  if (normalizedPhoneNumber.isEmpty()) {
    return null
  }
  return copy(
    phoneNumber = normalizedPhoneNumber,
    body = body.trimToNull(),
  )
}

internal fun AppStoreDetailsNavigationRequest.normalizedAndroidPackageName(): String? {
  return androidPackageName.trimToNull()
}

internal fun AppStoreDetailsNavigationRequest.normalizedIosAppId(): String? {
  return iosAppId.trimToNull()
}

internal fun String?.trimToNull(): String? {
  return this?.trim()?.takeIf { value -> value.isNotEmpty() }
}

internal fun invalidInputResult(): AppNavigationResult {
  return AppNavigationResult.Failed(AppNavigationFailureReason.INVALID_INPUT)
}
