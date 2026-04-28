package com.airsaid.toolkit

/**
 * Represents app-level metadata that is stable during runtime.
 *
 * @property packageName Android package name or iOS bundle identifier.
 * @property appName User-visible application name.
 * @property versionName User-visible version name.
 * @property buildNumber Internal build number.
 * @property buildType Build type from build configuration.
 * @property buildTime Build timestamp in ISO-8601 format.
 */
data class AppInfo(
  val packageName: String,
  val appName: String = "",
  val versionName: String,
  val buildNumber: String,
  val buildType: String = "",
  val buildTime: String = "",
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
