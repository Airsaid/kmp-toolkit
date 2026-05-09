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
      as? String
    val buildNumber = bundle.objectForInfoDictionaryKey("CFBundleVersion")
      as? String
    val appName = (bundle.objectForInfoDictionaryKey("CFBundleDisplayName") as? String)
      ?: (bundle.objectForInfoDictionaryKey("CFBundleName") as? String)
    val packageName = bundle.bundleIdentifier ?: ""
    return AppInfo(
      packageName = packageName,
      appName = appName?.takeUnless { it.isBlank() },
      versionName = versionName?.takeUnless { it.isBlank() },
      buildNumber = buildNumber?.takeUnless { it.isBlank() },
    )
  }
}
