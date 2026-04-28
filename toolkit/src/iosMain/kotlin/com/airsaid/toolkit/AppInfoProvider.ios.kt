package com.airsaid.toolkit

import platform.Foundation.NSBundle

/**
 * iOS implementation of [AppInfoProvider].
 */
internal actual object AppInfoProvider {

  /**
   * Returns current [AppInfo] for iOS.
   */
  actual fun getAppInfo(): AppInfo {
    val bundle = NSBundle.mainBundle
    val versionName = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString")
      as? String ?: ""
    val buildNumber = bundle.objectForInfoDictionaryKey("CFBundleVersion")
      as? String ?: ""
    val appName = (bundle.objectForInfoDictionaryKey("CFBundleDisplayName") as? String)
      ?: (bundle.objectForInfoDictionaryKey("CFBundleName") as? String)
      ?: ""
    val packageName = bundle.bundleIdentifier ?: ""
    return AppInfo(
      packageName = packageName,
      appName = appName,
      versionName = versionName,
      buildNumber = buildNumber,
      buildType = BuildKonfig.BUILD_TYPE,
      buildTime = BuildKonfig.BUILD_TIME,
    )
  }
}
