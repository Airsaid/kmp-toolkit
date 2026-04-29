package com.airsaid.toolkit

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Android implementation of [AppInfoProvider].
 */
internal actual object AppInfoProvider {

  private var applicationContext: Context? = null

  /**
   * Must be initialized with an Android [Context] before accessing [getAppInfo].
   */
  internal fun initialize(context: Context) {
    applicationContext = context.applicationContext
  }

  /**
   * Returns current [AppInfo] for Android.
   */
  actual fun getAppInfo(): AppInfo {
    val context = applicationContext
      ?: throw IllegalStateException(
        "AppInfoProvider must be initialized with Context on Android. " +
            "Call Toolkit.initialize(context) first."
      )
    val packageManager = context.packageManager
    val packageName = context.packageName
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
      @Suppress("DEPRECATION")
      packageManager.getPackageInfo(packageName, 0)
    }
    val versionName = packageInfo.versionName.orEmpty()
    val buildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      packageInfo.longVersionCode.toString()
    } else {
      @Suppress("DEPRECATION")
      packageInfo.versionCode.toString()
    }
    val applicationInfo = packageInfo.applicationInfo ?: run {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
      } else {
        @Suppress("DEPRECATION")
        packageManager.getApplicationInfo(packageName, 0)
      }
    }
    val appName = packageManager.getApplicationLabel(applicationInfo).toString()
    val buildType = if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
      "debug"
    } else {
      "release"
    }
    return AppInfo(
      packageName = packageName,
      appName = appName,
      versionName = versionName,
      buildNumber = buildNumber,
      buildType = buildType,
      buildTime = BuildKonfig.BUILD_TIME,
    )
  }
}
