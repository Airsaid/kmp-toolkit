package com.airsaid.toolkit

/**
 * Represents app-level metadata that is stable during runtime.
 *
 * @property packageName Android application id or iOS bundle identifier. Returns an empty string on iOS
 *   when the bundle identifier is unavailable.
 * @property appName User-visible application name, or `null` when unavailable.
 * @property versionName User-visible version name, or `null` when unavailable.
 * @property buildNumber Internal build number, or `null` when unavailable.
 */
data class AppInfo(
  val packageName: String,
  val appName: String? = null,
  val versionName: String? = null,
  val buildNumber: String? = null,
)

/**
 * Provides platform-specific app metadata.
 *
 * Android requires [Toolkit.initialize] before calling [getAppInfo].
 */
internal expect object AppInfoProvider {

  /**
   * Returns current [AppInfo].
   */
  fun getAppInfo(): AppInfo
}
