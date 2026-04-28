@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.airsaid.toolkit

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSLocaleLanguageCode
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.localTimeZone
import platform.Foundation.preferredLanguages
import platform.Foundation.secondsFromGMT
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceIdiomPad
import kotlinx.cinterop.useContents
import kotlin.math.roundToInt

/**
 * iOS implementation of [DeviceInfoProvider].
 */
internal actual object DeviceInfoProvider {

  /**
   * Returns current [DeviceInfo] for iOS.
   */
  actual fun getDeviceInfo(): DeviceInfo {
    val device = UIDevice.currentDevice
    val locale = NSLocale.Companion.currentLocale
    val languageCode = locale.objectForKey(NSLocaleLanguageCode) as? String
    val regionCode = locale.objectForKey(NSLocaleCountryCode) as? String
    val currentLocale = buildLocaleInfo(languageCode, regionCode)
    val preferredLocales = buildPreferredLocales(currentLocale)

    val screen = UIScreen.mainScreen
    val bounds = screen.bounds
    val scale = screen.scale
    var widthDp = 0
    var heightDp = 0
    var isLandscape = false
    bounds.useContents {
      widthDp = size.width.roundToInt()
      heightDp = size.height.roundToInt()
      isLandscape = size.width >= size.height
    }
    val widthPx = (widthDp * scale).roundToInt()
    val heightPx = (heightDp * scale).roundToInt()
    val density = scale.toFloat()
    val densityDpi = (density * 160f).roundToInt()

    val timeZone = NSTimeZone.Companion.localTimeZone
    val timeZoneOffsetMinutes = (timeZone.secondsFromGMT / 60).toInt()

    return DeviceInfo(
      deviceModel = device.model,
      systemName = device.systemName,
      systemVersion = device.systemVersion,
      systemVersionCode = -1,
      manufacturer = ManufacturerInfo(
        manufacturer = "Apple",
        brand = "Apple",
      ),
      deviceType = DeviceTypeInfo(
        isTablet = device.userInterfaceIdiom == UIUserInterfaceIdiomPad,
        isEmulator = isSimulator(),
      ),
      screen = ScreenInfo(
        widthPx = widthPx,
        heightPx = heightPx,
        widthDp = widthDp,
        heightDp = heightDp,
        density = density,
        densityDpi = densityDpi,
        isLandscape = isLandscape,
      ),
      timeZone = TimeZoneInfo(
        id = timeZone.name ?: "",
        offsetMinutes = timeZoneOffsetMinutes,
      ),
      locale = LocaleBundle(
        current = currentLocale,
        preferred = preferredLocales,
      ),
    )
  }
}

private fun buildPreferredLocales(currentLocale: LocaleInfo): List<LocaleInfo> {
  val preferred = (NSLocale.Companion.preferredLanguages as? List<*>)?.mapNotNull { entry ->
    val tag = entry as? String
    buildLocaleInfoFromTag(tag)
  }.orEmpty()
  return if (preferred.isNotEmpty()) preferred else listOf(currentLocale)
}

private fun isSimulator(): Boolean {
  val environment = NSProcessInfo.processInfo.environment
  return environment["SIMULATOR_DEVICE_NAME"] != null ||
    environment["SIMULATOR_MODEL_IDENTIFIER"] != null
}
